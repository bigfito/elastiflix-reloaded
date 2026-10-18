package com.bigfito.elastiflix.ingest.file;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.IngestValidationException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Nombre, tamaño y UTF-8 de un archivo de carga.
 */
class FileCheckerTest {

    private static final DataSize LIMIT = DataSize.ofMegabytes(150);
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private final FileChecker checker = new FileChecker();

    @ParameterizedTest
    @ValueSource(strings = {"elastiflix-reloaded.json", "peliculas_2026.csv", "CATALOGO.JSON"})
    @DisplayName("acepta json y csv, también en mayúsculas")
    void shouldAcceptJsonAndCsvWhenTheNameHasOneDot(String fileName) {
        CheckedFile checked = checker.check(candidate(fileName, text("[]")), LIMIT);

        assertThat(checked.fileName()).isEqualTo(fileName);
        if (fileName.toLowerCase().endsWith(".json")) {
            assertThat(checked.kind()).isEqualTo(FileKind.JSON);
        } else {
            assertThat(checked.kind()).isEqualTo(FileKind.CSV);
        }
    }

    @Test
    @DisplayName("rechaza un txt, un nombre sin punto y las dobles extensiones, cada uno con su código")
    void shouldRejectTheNamesListedInThePlan() {
        assertCode("peliculas.txt", ErrorCode.BAD_EXTENSION);
        assertCode("peliculas", ErrorCode.BAD_EXTENSION);
        assertCode("peliculas.csv.exe", ErrorCode.DOUBLE_EXTENSION);
        assertCode("peliculas.exe.csv", ErrorCode.DOUBLE_EXTENSION);
        assertCode("peliculas.json.csv", ErrorCode.DOUBLE_EXTENSION);
    }

    @Test
    @DisplayName("un byte por encima de 150 MB se rechaza sin leer el contenido")
    void shouldRejectOneByteOverTheLimitWithoutReading() {
        InputStream trap = new InputStream() {
            @Override
            public int read() {
                throw new AssertionError("no debía leer el cuerpo");
            }
        };
        FileCandidate candidate = new FileCandidate("grande.json", LIMIT.toBytes() + 1, trap);

        assertThatThrownBy(() -> checker.check(candidate, LIMIT))
                .isInstanceOf(IngestValidationException.class)
                .extracting(thrown -> ((IngestValidationException) thrown).errorCode())
                .isEqualTo(ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    @DisplayName("el tamaño exacto de 150 MB pasa a la comprobación de UTF-8")
    void shouldAcceptTheExactLimitWhenTheTextIsUtf8() {
        FileCandidate candidate = new FileCandidate("justo.json", LIMIT.toBytes(), text(""));

        CheckedFile checked = checker.check(candidate, LIMIT);

        assertThat(checked.kind()).isEqualTo(FileKind.JSON);
    }

    @Test
    @DisplayName("rechaza bytes que no son UTF-8")
    void shouldRejectBytesThatAreNotUtf8() {
        byte[] broken = {(byte) 0xC3, (byte) 0x28};
        FileCandidate candidate = new FileCandidate("roto.json", broken.length, new ByteArrayInputStream(broken));

        assertThatThrownBy(() -> checker.check(candidate, LIMIT))
                .isInstanceOf(IngestValidationException.class)
                .extracting(thrown -> ((IngestValidationException) thrown).errorCode())
                .isEqualTo(ErrorCode.INVALID_UTF8);
    }

    @Test
    @DisplayName("acepta UTF-8 con BOM y un archivo vacío")
    void shouldAcceptABomAndAnEmptyFile() {
        byte[] withBom = new byte[UTF8_BOM.length + 2];
        System.arraycopy(UTF8_BOM, 0, withBom, 0, UTF8_BOM.length);
        withBom[UTF8_BOM.length] = 'o';
        withBom[UTF8_BOM.length + 1] = 'k';

        assertThat(checker.check(candidate("con-bom.json", new ByteArrayInputStream(withBom)), LIMIT).kind())
                .isEqualTo(FileKind.JSON);
        assertThat(checker.check(candidate("vacio.csv", text("")), LIMIT).kind()).isEqualTo(FileKind.CSV);
    }

    private void assertCode(String fileName, ErrorCode code) {
        assertThatThrownBy(() -> checker.check(candidate(fileName, text("")), LIMIT))
                .isInstanceOf(IngestValidationException.class)
                .extracting(thrown -> ((IngestValidationException) thrown).errorCode())
                .isEqualTo(code);
    }

    private static FileCandidate candidate(String fileName, InputStream content) {
        return new FileCandidate(fileName, 4, content);
    }

    private static InputStream text(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
