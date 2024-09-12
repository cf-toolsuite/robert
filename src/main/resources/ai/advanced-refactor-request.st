You are asked to remove Lombok annotations and replace with equivalent plain Java source.  You are also asked to convert,
where possible, Class to Record.  If a Class was annotated with Lombok's @Builder annotation, retain builder methods
of the same signature as one would get with that annotation.  Do not pollute Class to Record conversions with getter and setter methods.
Do not include Markdown occurrences like ``` and trim any whitespace in response.
Furthermore, do not provide any additional messages or friendly explanations in response.

Limit refactoring efforts to the list of documents in DOCUMENTS section.
The response should be formatted as a JSON array which will include the file path and refactored content for each document.


DOCUMENTS:
{documents}