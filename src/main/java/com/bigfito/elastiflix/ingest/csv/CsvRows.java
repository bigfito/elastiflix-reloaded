package com.bigfito.elastiflix.ingest.csv;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.IngestValidationException;

/**
 * Filas de un CSV. Una celda entre comillas puede ocupar varias líneas.
 */
final class CsvRows implements AutoCloseable {

    private static final char QUOTE = '"';
    private static final char LF = '\n';
    private static final char CR = '\r';

    private final PushbackReader reader;
    private final char separator;
    private boolean finished;

    CsvRows(Reader reader, char separator) {
        this.reader = new PushbackReader(reader);
        this.separator = separator;
    }

    /**
     * Siguiente fila. Vacío cuando el archivo ya terminó.
     *
     * @return celdas, en orden
     */
    Optional<List<String>> nextRow() {
        if (finished) {
            return Optional.empty();
        }
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean started = false;
        while (true) {
            int ch = read();
            if (ch < 0) {
                return end(row, cell, started);
            }
            started = true;
            if (ch == QUOTE) {
                cell.append(quoted());
            } else if (ch == separator) {
                row.add(cell.toString());
                cell.setLength(0);
            } else if (ch == LF || ch == CR) {
                return lineBreak(row, cell, ch);
            } else {
                cell.append((char) ch);
            }
        }
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    private Optional<List<String>> lineBreak(List<String> row, StringBuilder cell, int ch) {
        if (ch == CR) {
            skipLf();
        }
        row.add(cell.toString());
        return Optional.of(row);
    }

    private Optional<List<String>> end(List<String> row, StringBuilder cell, boolean started) {
        finished = true;
        if (!started) {
            return Optional.empty();
        }
        row.add(cell.toString());
        return Optional.of(row);
    }

    private String quoted() {
        StringBuilder text = new StringBuilder();
        while (true) {
            int ch = read();
            if (ch < 0) {
                throw new IngestValidationException(ErrorCode.UNREADABLE_CSV);
            }
            if (ch == QUOTE && !escapedQuote(text)) {
                return text.toString();
            }
            appendChar(text, ch);
        }
    }

    private void appendChar(StringBuilder text, int ch) {
        if (ch == QUOTE) {
            return;
        }
        if (ch == CR) {
            text.append(breakChar());
            return;
        }
        text.append((char) ch);
    }

    private char breakChar() {
        int next = read();
        if (next == LF) {
            return LF;
        }
        if (next >= 0) {
            unread(next);
        }
        return CR;
    }

    private boolean escapedQuote(StringBuilder text) {
        int next = read();
        if (next == QUOTE) {
            text.append(QUOTE);
            return true;
        }
        if (next >= 0) {
            unread(next);
        }
        return false;
    }

    private void skipLf() {
        int next = read();
        if (next != LF && next >= 0) {
            unread(next);
        }
    }

    private int read() {
        try {
            return reader.read();
        } catch (IOException exception) {
            throw new IngestValidationException(ErrorCode.UNREADABLE_CSV, exception);
        }
    }

    private void unread(int ch) {
        try {
            reader.unread(ch);
        } catch (IOException exception) {
            throw new IngestValidationException(ErrorCode.UNREADABLE_CSV, exception);
        }
    }
}
