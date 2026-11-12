package com.bigfito.elastiflix.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JudgmentFileTest {

    @TempDir
    Path folder;

    @Test
    @DisplayName("lee cada línea como una consulta y salta las vacías")
    void shouldReadOneJudgmentPerLine() throws IOException {
        Path file = write("""
                {"id":"es-plot-01","lang":"es","kind":"plot","text":"un barco","target":"597","ratings":{"597":3,"1":0}}

                {"id":"en-title-01","lang":"en","kind":"title","text":"star wars","target":"11","ratings":{"11":3}}
                """);

        List<Judgment> judgments = JudgmentFile.read(file);

        assertThat(judgments).extracting(Judgment::id).containsExactly("es-plot-01", "en-title-01");
        assertThat(judgments.getFirst().ratings()).containsEntry("597", 3).containsEntry("1", 0);
    }

    @Test
    @DisplayName("rechaza un id repetido")
    void shouldRejectRepeatedIds() throws IOException {
        Path file = write("""
                {"id":"q","lang":"es","kind":"plot","text":"a","target":"1","ratings":{"1":3}}
                {"id":"q","lang":"es","kind":"plot","text":"b","target":"2","ratings":{"2":3}}
                """);

        assertThatThrownBy(() -> JudgmentFile.read(file)).hasMessageContaining("repetida");
    }

    @Test
    @DisplayName("exige que la película buscada tenga 3")
    void shouldRequireTheTargetToBeRatedThree() throws IOException {
        Path file = write("""
                {"id":"q","lang":"es","kind":"plot","text":"a","target":"1","ratings":{"1":2}}
                """);

        assertThatThrownBy(() -> JudgmentFile.read(file)).hasMessageContaining("calificación 3");
    }

    @Test
    @DisplayName("rechaza calificaciones fuera de 0 a 3")
    void shouldRejectRatingsOutOfRange() throws IOException {
        Path file = write("""
                {"id":"q","lang":"es","kind":"plot","text":"a","target":"1","ratings":{"1":3,"2":4}}
                """);

        assertThatThrownBy(() -> JudgmentFile.read(file)).hasMessageContaining("va de 0 a 3");
    }

    @Test
    @DisplayName("el conjunto versionado tiene 40 consultas equilibradas por idioma")
    void shouldKeepTheVersionedSetBalanced() {
        List<Judgment> judgments = JudgmentFile.read(JudgmentFile.DEFAULT);

        assertThat(judgments).hasSize(40);
        assertThat(judgments).filteredOn(judgment -> judgment.lang().equals("es")).hasSize(20);
        assertThat(judgments).filteredOn(judgment -> judgment.kind().equals("plot")).hasSize(30);
        assertThat(judgments).allSatisfy(judgment ->
                assertThat(judgment.kind()).isIn("plot", "title"));
    }

    private Path write(String content) throws IOException {
        Path file = folder.resolve("judgments.jsonl");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}
