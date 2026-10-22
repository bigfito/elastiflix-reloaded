package com.bigfito.elastiflix.ingest.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.IngestValidationException;

/**
 * Recorre un archivo y rechaza los bytes que no son UTF-8.
 *
 * <p>El BOM {@code EF BB BF} es válido y se quita. Un archivo vacío es UTF-8.</p>
 */
final class Utf8Decoder {

    private static final int CHUNK = 4096;
    private static final int BOM_LENGTH = 3;
    private static final int FIRST_BOM_BYTE = 0xEF;
    private static final int SECOND_BOM_BYTE = 0xBB;
    private static final int THIRD_BOM_BYTE = 0xBF;

    private Utf8Decoder() {
    }

    /**
     * Lee el flujo hasta el final. No guarda el texto.
     *
     * @param content bytes del archivo
     */
    static void requireUtf8(InputStream content) {
        if (content == null) {
            throw new IngestValidationException(ErrorCode.INVALID_UTF8);
        }
        try (PushbackInputStream input = withoutBom(content)) {
            decode(input);
        } catch (IOException exception) {
            throw new IngestValidationException(ErrorCode.INVALID_UTF8, exception);
        }
    }

    private static PushbackInputStream withoutBom(InputStream content) throws IOException {
        PushbackInputStream input = new PushbackInputStream(content, BOM_LENGTH);
        byte[] prefix = input.readNBytes(BOM_LENGTH);
        if (!isBom(prefix)) {
            input.unread(prefix);
        }
        return input;
    }

    private static boolean isBom(byte[] prefix) {
        return prefix.length == BOM_LENGTH
                && unsigned(prefix[0]) == FIRST_BOM_BYTE
                && unsigned(prefix[1]) == SECOND_BOM_BYTE
                && unsigned(prefix[2]) == THIRD_BOM_BYTE;
    }

    private static int unsigned(byte value) {
        return Byte.toUnsignedInt(value);
    }

    private static void decode(InputStream input) throws IOException {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        ByteBuffer pending = ByteBuffer.allocate(CHUNK);
        CharBuffer ignored = CharBuffer.allocate(CHUNK);
        byte[] chunk = new byte[CHUNK];
        int read = input.read(chunk);
        while (read >= 0) {
            pending = append(pending, chunk, read);
            decodeChunk(decoder, pending, ignored, false);
            read = input.read(chunk);
        }
        decodeChunk(decoder, pending, ignored, true);
        throwIfError(decoder.flush(ignored));
    }

    private static ByteBuffer append(ByteBuffer pending, byte[] chunk, int read) {
        ByteBuffer target = pending;
        if (target.remaining() < read) {
            target = grow(target, read);
        }
        target.put(chunk, 0, read);
        return target;
    }

    private static ByteBuffer grow(ByteBuffer pending, int read) {
        ByteBuffer larger = ByteBuffer.allocate(pending.position() + read);
        pending.flip();
        larger.put(pending);
        return larger;
    }

    private static void decodeChunk(
            CharsetDecoder decoder, ByteBuffer pending, CharBuffer ignored, boolean end)
            throws CharacterCodingException {
        pending.flip();
        throwIfError(decoder.decode(pending, ignored, end));
        pending.compact();
        ignored.clear();
    }

    private static void throwIfError(CoderResult result) throws CharacterCodingException {
        if (result.isError()) {
            result.throwException();
        }
    }
}
