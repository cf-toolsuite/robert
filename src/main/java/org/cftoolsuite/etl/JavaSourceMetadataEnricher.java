package org.cftoolsuite.etl;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

@Component
public class JavaSourceMetadataEnricher implements DocumentTransformer {

    public static final String SOURCE_METADATA_KEY = "source";
	private static final String DECOMPOSITION_METADATA_KEY = "decomposition";

	public static final String CONTEXT_STR_PLACEHOLDER = "source";

	public static final String TEMPLATE = """
        You are asked to parse Java source. You will return the package, set of imports, type (for example: class, interface, enum, annotation, record),
        a set of constructors with modifiers and signatures, a set of member variables and their modifiers, a set of methods with modifiers and signatures.
        In addition, you will return a set of all external method calls.
        Please return this in JSON format.

        Here is the source:

        {source}.  """;

	private final ChatModel chatModel;

	public JavaSourceMetadataEnricher(ChatModel chatModel) {
		Assert.notNull(chatModel, "ChatModel must not be null");
		this.chatModel = chatModel;
	}

	@Override
	public List<Document> apply(List<Document> documents) {
        var template = new PromptTemplate(TEMPLATE);
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
