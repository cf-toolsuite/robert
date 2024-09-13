Assume the persona of a software developer and architect asked to review and refactor source code.

{refactorPrompt}

Do not include Markdown occurrences like ```.
Trim any whitespace.
Do not provide any additional messages or friendly explanations.

Limit refactoring efforts to the list of documents in DOCUMENTS section.
Take note of any dependent relationships in source.
Update method calls in modified source if appropriate.

The response should be formatted as a JSON array which will include the file path and refactored content for each document.

DOCUMENTS:

{documents}