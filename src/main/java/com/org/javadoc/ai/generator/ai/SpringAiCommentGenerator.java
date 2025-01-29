package com.org.javadoc.ai.generator.ai;

import com.org.javadoc.ai.generator.ai.client.AiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TODO: Add class description here.
 */
@Component
public class SpringAiCommentGenerator {

    private static final Logger logger = LoggerFactory.getLogger(SpringAiCommentGenerator.class);

    @Autowired
    private AiClient aiClient;

    public String generateClassComment(String classBody, String className) {
        logger.info("Calling AI Client for class description");
        String systemPrompt = """
            You are an intelligent Java documentation assistant. Your task is to generate precise and clean Javadoc-style comments for the provided Java class.
            Instructions:
            1. Class-Level Comments: Provide a concise overview of the class, describing its purpose, functionality, and key features. Avoid unnecessary verbosity or repetition.
            2. Include Existing Code: Maintain the exact structure and syntax of the provided Java class, ensuring the code remains intact and error-free.
            3. No Compilation Errors: Ensure the class, along with the generated comments, is syntactically correct and will not cause any compilation issues.
            4. Documentation Style: Use proper Javadoc syntax for all comments. Avoid over-documentation—focus on clarity and precision.            
            Output Format:
            /**
             * [Briefly describe the purpose of the class, its functionality, and key features.]
             * [Mention any specific use cases or relevant details if applicable.]
             */
            public class [ClassName] {
                // Preserve the original class structure and code here.
            }
            Note: Always prioritize clarity and maintain the existing code structure.
            """;
        return aiClient.callApi(systemPrompt, classBody);
    }

    public String generateMethodComment(String code, String className) {
        logger.info("Calling AI Client for method description for class: {}", className);
        String systemPrompt = """
            You are an intelligent Java documentation assistant. Your task is to generate precise and clean Javadoc-style comments for the provided Java method.
            Instructions:
            1. Method-Level Comments: Provide a concise description of the method, explaining its purpose, functionality, and any important details.
            2. Parameters: Document all parameters using @param, briefly explaining their purpose and significance.
            3. Return Value: If the method has a return value, describe it using @return.
            4. Exceptions: If the method throws exceptions, document them using @throws, explaining when they may occur.
            5. Include Existing Code: Maintain the exact structure and syntax of the provided method, ensuring the code remains intact and error-free.
            6. No Compilation Errors: Ensure the method, along with the generated comments, is syntactically correct and will not cause any compilation issues.
            7. Documentation Style: Use proper Javadoc syntax for all comments. Avoid over-documentation—focus on clarity and precision.
            Output Format:
            /**
             * [Describe the purpose of the method, its functionality, and key details.]
             * 
             * @param [parameterName] [Brief description of the parameter.]
             * @return [Brief description of the return value, if applicable.]
             * @throws [ExceptionName] [Brief description of the exception, if applicable.]
             */
            public [ReturnType] [MethodName](...) {
                // Preserve the original method structure and code here.
            }
            """;
        return aiClient.callApi(systemPrompt, code);
    }

    public String fixSonarIssues(String className, String classBody, Set<String> descriptions) {
        logger.info("Calling AI Client for fixing multiple SonarQube issues for class: {}", className);

        // Joining multiple issue descriptions into a formatted bullet list
        String formattedIssues = descriptions.stream()
                .map(desc -> "- " + desc)
                .collect(Collectors.joining("\n"));

        String systemPrompt = String.format(
                "You are an expert Java developer and code quality analyzer. Your task is to analyze and fix the provided Java class **%s** based on the reported SonarQube issues. "
                        + "All issue descriptions for this class are provided below. Ensure that all issues are **accurately identified and fixed** in a single pass.\n\n"
                        + "**SonarQube Issues to Fix:**\n%s\n\n"
                        + "### **Strict Guidelines:**\n"
                        + "1. **Scope of Analysis:**\n"
                        + "    - Focus exclusively on the class **%s**.\n"
                        + "    - Do not assume or modify external classes, dependencies, or configurations unless explicitly provided.\n"
                        + "2. **Issue Resolution Requirements:**\n"
                        + "    - Carefully review and fix **each** issue listed above.\n"
                        + "    - Apply best practices to resolve the issues effectively.\n"
                        + "    - Ensure the fixes do not introduce new bugs or change the intended functionality.\n"
                        + "3. **Code Quality & Compliance:**\n"
                        + "    - Follow Java coding standards, maintainability, and readability best practices.\n"
                        + "    - Ensure the final class is fully **compilable and functional**.\n"
                        + "    - Avoid unnecessary code modifications beyond the reported issues.\n"
                        + "4. **Commenting Guidelines:**\n"
                        + "    - Add **very concise comments** only where necessary to explain changes.\n"
                        + "    - Do not over-comment or include redundant explanations.\n"
                        + "5. **Error Handling & Insufficient Information:**\n"
                        + "    - If some issues cannot be fully resolved due to missing context, return the original code **with explanatory comments** highlighting what additional details are required.\n"
                        + "6. **Output Format:**\n"
                        + "    - Return **only the corrected Java class with proper formatting**.\n"
                        + "    - **Do not include any additional text, explanations, or formatting outside the class definition.**\n"
                        +"     - **Strictly avoid** including backticks, fences (`), pseudo-code, or incomplete code snippets.\n"
                        + "7. **Java Version Compatibility:**\n"
                        + "    - Ensure compatibility with **Java 8 and above**.\n"
                        + "    - Avoid deprecated APIs unless explicitly necessary.\n"
                        + "    - Follow modern Java practices while maintaining backward compatibility where required.\n",
                className, formattedIssues, className
        );

        return aiClient.callApi(systemPrompt, classBody);
    }
}
