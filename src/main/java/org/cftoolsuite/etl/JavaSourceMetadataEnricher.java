package org.cftoolsuite.etl;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class JavaSourceMetadataEnricher implements DocumentTransformer {

    private static final Logger log = LoggerFactory.getLogger(JavaSourceMetadataEnricher.class);

    private final ObjectMapper objectMapper;

    public JavaSourceMetadataEnricher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

	@Override
	public List<Document> apply(List<Document> documents) {
		try {
            for (Document document : documents) {
                if (!CollectionUtils.isEmpty(document.getMetadata()) && document.getMetadata().containsKey("source")) {
                    if (( (String) document.getMetadata().get("source")).endsWith(".java")) {
                        log.info("-- Enriching Java source metadata for: {}", document.getMetadata().get("source"));
                        ObjectNode node = parse(document.getContent());
                        String[] jsonKeys = new String[] { "package", "imports", "type", "externalMethodCalls" };
                        for (String jsonKey: jsonKeys) {
                            document.getMetadata().put(jsonKey, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node.get(jsonKey)));
                        }
                    }
                }
            }
            return documents;
        } catch (Exception e) {
            throw new RuntimeException("Error while enriching Java source metadata", e);
        }

	}

    protected ObjectNode parse(String content) throws IOException {
        CompilationUnit cu = StaticJavaParser.parse(content);

        ObjectNode jsonNode = objectMapper.createObjectNode();

        // Package
        cu.getPackageDeclaration().ifPresent(pkg ->
            jsonNode.put("package", pkg.getNameAsString())
        );

        // Imports
        ArrayNode importsNode = jsonNode.putArray("imports");
        cu.getImports().forEach(imp ->
            importsNode.add(imp.getNameAsString())
        );

        // Type (class, interface, enum, annotation, record)
        cu.getTypes().forEach(type -> {
            ObjectNode typeNode = jsonNode.putObject("type");
            typeNode.put("name", type.getNameAsString());
            typeNode.put("kind", getTypeKind(type));

            // Constructors
            ArrayNode constructorsNode = typeNode.putArray("constructors");
            type.getConstructors().forEach(constructor ->
                constructorsNode.add(getMethodSignature(constructor))
            );

            // Member variables
            ArrayNode fieldsNode = typeNode.putArray("fields");
            type.getFields().forEach(field -> {
                ObjectNode fieldNode = fieldsNode.addObject();
                fieldNode.put("name", field.getVariables().get(0).getNameAsString());
                fieldNode.put("type", field.getCommonType().asString());
                fieldNode.put("modifiers", field.getModifiers().toString());
            });

            // Methods
            ArrayNode methodsNode = typeNode.putArray("methods");
            type.getMethods().forEach(method ->
                methodsNode.add(getMethodSignature(method))
            );
        });

        // External method calls
        Set<String> externalCalls = new HashSet<>();
        cu.accept(new MethodCallVisitor(), externalCalls);
        ArrayNode externalCallsNode = jsonNode.putArray("externalMethodCalls");
        externalCalls.forEach(externalCallsNode::add);

        return jsonNode;
    }

    private static String getTypeKind(TypeDeclaration<?> type) {
        if (type instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration coid = (ClassOrInterfaceDeclaration) type;
            return coid.isInterface() ? "interface" : "class";
        } else if (type instanceof EnumDeclaration) {
            return "enum";
        } else if (type instanceof AnnotationDeclaration) {
            return "annotation";
        } else if (type.getClass().getSimpleName().equals("RecordDeclaration")) {
            return "record";
        }
        return "unknown";
    }

    private static String getMethodSignature(CallableDeclaration<?> method) {
        return String.format("%s %s%s",
            method.getModifiers(),
            method.getNameAsString(),
            method.getParameters()
        );
    }

    private static class MethodCallVisitor extends VoidVisitorAdapter<Set<String>> {
        @Override
        public void visit(MethodCallExpr n, Set<String> collector) {
            super.visit(n, collector);
            n.getScope().ifPresent(scope ->
                collector.add(scope + "." + n.getNameAsString())
            );
        }
    }

}