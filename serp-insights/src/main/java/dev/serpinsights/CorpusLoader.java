package dev.serpinsights;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
class CorpusLoader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    List<SourceDocument> load(String classpathJsonFile) {
        try (InputStream in = new ClassPathResource(classpathJsonFile).getInputStream()) {
            return objectMapper.readValue(in, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, SourceDocument.class));
        } catch (IOException e) {
            throw new IllegalStateException("Could not load corpus: " + classpathJsonFile, e);
        }
    }
}
