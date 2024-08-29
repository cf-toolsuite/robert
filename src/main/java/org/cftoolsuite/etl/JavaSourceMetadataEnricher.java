package org.cftoolsuite.etl;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

public class JavaSourceMetadataEnricher implements DocumentTransformer {

    public static final String SOURCE_METADATA_KEY = "source";
	private static final String DECOMPOSITION_METADATA_KEY = "decomposition";

	public static final String CONTEXT_STR_PLACEHOLDER = "source";

	private final ChatModel chatModel;
	private final Resource templateFile;

	public JavaSourceMetadataEnricher(ChatModel chatModel, Resource templateFile) {
		Assert.notNull(chatModel, "ChatModel must not be null");
		this.chatModel = chatModel;
        this.templateFile = templateFile;
	}

	@Override
	public List<Document> apply(List<Document> documents) {
        var template = new PromptTemplate(templateFile);
		for (Document document : documents) {
            if (!CollectionUtils.isEmpty(document.getMetadata()) && document.getMetadata().containsKey(SOURCE_METADATA_KEY)) {
                if (( (String) document.getMetadata().get(SOURCE_METADATA_KEY)).endsWith(".java")) {
                    Prompt prompt = template.create(Map.of(CONTEXT_STR_PLACEHOLDER, document.getContent()));
                    String decomposition = this.chatModel.call(prompt).getResult().getOutput().getContent();
                    document.getMetadata().putAll(Map.of(DECOMPOSITION_METADATA_KEY, decomposition));
                }
            }
		}
		return documents;
	}

}
