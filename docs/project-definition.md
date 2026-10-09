# Project Definition: Kees

Status: Draft

## 1. Purpose

This project is a personal, AI-driven technical knowledge base for collecting,
searching, and querying information about software development. Example subjects
include programming languages, data structures, software engineering theory, and
framework-specific documentation such as Java, Spring Boot, and Vue.

The product is named **Kees**. Kees is also the user-facing conversational
identity through which the user interacts with the knowledge base.

The project has two equally important outcomes:

1. Build a useful personal knowledge base.
2. Learn Java, Spring Boot, information retrieval, vector search, and
   retrieval-augmented generation (RAG) by implementing them incrementally.

The application is initially a local, single-user system. It should stay simple
enough to understand while leaving room for later expansion.

## 2. Learning Goals

The project should help develop a practical understanding of:

- idiomatic Java and relevant language features;
- Spring Boot conventions and dependency injection;
- REST API design;
- persistence and transaction boundaries;
- domain modelling and modular application architecture;
- focused unit testing of important domain logic;
- document ingestion and text processing;
- full-text, semantic, and hybrid retrieval;
- embeddings and vector storage;
- the construction, evaluation, and limitations of RAG systems.

Important implementation decisions should be accompanied by:

1. the selected approach;
2. the reason for selecting it;
3. reasonable alternatives;
4. the conditions under which the decision should be reconsidered.

It should also be made clear whether a decision follows from Java, from Spring,
or from the architecture chosen for this project.

## 3. Initial User and Context

- The initial user is the developer building the application.
- The application runs locally.
- Authentication and authorization are not part of the MVP.
- Authentication becomes relevant before the application is exposed outside the
  local development environment.
- The application code and user interface use English.
- Documents and questions may be in English or Dutch.
- The assistant is presented to the user as Kees.
- Kees may have a friendly and recognizable voice, but must remain transparent
  about sources, missing information, and uncertainty.

## 4. MVP User Journey

The primary user journey is:

1. Create or select a collection, such as `Java`, `Spring Boot`, or
   `Data Structures`.
2. Upload one or more Markdown documents.
3. Let the application parse, split, and index the documents.
4. Browse or search the stored documents.
5. Ask a technical question.
6. Receive an answer generated from relevant stored passages.
7. Inspect the passages and source locations used for the answer.

## 5. MVP Scope

### 5.1 Document ingestion

- Upload one or more `.md` files.
- Preserve the original document and its relevant metadata.
- Recognize Markdown structure, including headings, lists, and code blocks.
- Recognize optional frontmatter when present.
- Use headings as metadata when creating searchable chunks.
- Track ingestion status and failures.
- Images and file attachments embedded in Markdown are out of scope for the MVP.

### 5.2 Organization

- A document can belong to one collection.
- Collections represent broad subjects such as Java or Spring Boot.
- Free-form tags and nested taxonomies are deferred until a concrete need arises.

### 5.3 Conventional search

- Search document titles and contents.
- Filter results by collection.
- Open a stored document.
- Show and highlight the matching passage.

### 5.4 RAG question answering

- Accept a single question at a time.
- Retrieve relevant passages from the stored documents.
- Generate an answer using the OpenAI API and only the supplied retrieval
  context where possible.
- Show the documents and exact passages used to produce the answer.
- Show the heading or location of each passage.
- Show a relevance score with enough explanation to avoid presenting it as a
  universal probability of correctness.
- Provide a learning/debug view of the retrieved chunks and assembled model
  context.

Conversational follow-up questions and long-lived chat history are not required
for the MVP.

### 5.5 User interface

The Vue and TypeScript interface may be visually minimal in the MVP. It needs to
support the core workflows clearly, but visual polish and a comprehensive design
system are not initial goals. The question-and-answer experience presents Kees as
the conversational counterpart while keeping retrieved evidence directly
inspectable.

## 6. Initial Domain Concepts

### Collection

A user-facing grouping of related documents.

### Source

Describes where information originated. In the MVP, a source represents an
uploaded Markdown file. Later source types may include URLs or synchronized
repositories.

### Document

The logical, user-visible document, including title, content, source information,
and metadata.

### Chunk

A derived passage from a document that can be indexed and retrieved independently.
A chunk is an implementation concept used by retrieval; it is not a replacement
for the user-visible document.

### Ingestion job

Tracks the processing of an uploaded source, including its current state and any
failure information.

Conversations and messages may become domain concepts later, but are not required
for single-question MVP behaviour.

## 7. High-Level Architecture

### Backend

- Java and Spring Boot.
- A modular monolith rather than separate services.
- Modules or explicit boundaries for at least:
  - collections and documents;
  - ingestion;
  - search and retrieval;
  - question answering.
- A REST API used by the frontend.
- An integration with the OpenAI API behind a small application-owned boundary.
- Only one AI provider implementation is required initially.

The modular monolith keeps local development and debugging simple while still
making responsibilities and dependencies visible.

### Frontend

- Vue with TypeScript.
- An English-language interface.
- A minimal document upload and browsing experience.
- A search view.
- A question-and-answer view with inspectable sources and retrieval details.

### Data and local infrastructure

- PostgreSQL for relational application data.
- A PostgreSQL vector extension for embeddings and similarity search.
- Database infrastructure runs through Docker Compose.
- The Spring Boot and Vue applications run directly during development to keep
  debugging straightforward.

The initial design deliberately keeps relational data and vectors in one database.
A dedicated vector database should only be considered when concrete requirements
justify the added operational and conceptual complexity.

## 8. Testing Strategy for the MVP

- Write unit tests for important domain behaviour.
- Avoid pursuing coverage as a goal by itself.
- Add broader integration or end-to-end testing later when database behaviour,
  framework wiring, or critical user flows make it valuable.

## 9. Delivery and Learning Phases

### Phase 1: Application foundation

Build a vertical slice for creating, storing, listing, and opening Markdown-based
documents without AI. Learn the basic Spring request flow and application
boundaries.

### Phase 2: Ingestion pipeline

Parse Markdown, preserve its structure, create chunks, and expose processing
status. Make each transformation inspectable.

### Phase 3: Conventional retrieval

Implement title and content search with collection filtering. Establish a useful
baseline before introducing embeddings.

### Phase 4: Embeddings and vector retrieval

Generate embeddings, store them alongside chunks, and retrieve chunks using
vector similarity. Explain what embeddings represent, how similarity is measured,
and what a vector index does and does not guarantee.

### Phase 5: First RAG flow

Retrieve passages for a question, assemble model context, call the OpenAI API,
and present an answer with inspectable citations.

### Phase 6: Retrieval quality

Compare retrieval approaches and introduce improvements only when measurements
or observed failures justify them. Possible later topics include hybrid search,
reranking, chunking experiments, and evaluation datasets.

### Phase 7: Expansion

Potential additions include URL ingestion, documentation-site crawling,
repository synchronization, conversational questions, authentication,
authorization, deployment, and more extensive testing.

## 10. RAG Learning Approach

RAG and vector storage are treated as subjects to understand, not infrastructure
to hide behind a library. Each stage should expose its inputs and outputs:

```text
Markdown document
    -> parsed structure
    -> chunks
    -> embeddings
    -> stored vectors

Question
    -> query embedding
    -> retrieved chunks
    -> assembled context
    -> model request
    -> answer with source evidence
```

Before adding optimizations, the project should answer questions such as:

- Why was a document divided at particular boundaries?
- What information is lost when text becomes an embedding?
- What does a similarity score mean for the chosen metric?
- Why were particular chunks retrieved or missed?
- How much context was sent to the model?
- Can every important claim in an answer be traced back to a source passage?
- How do we distinguish retrieval failure from generation failure?

## 11. MVP Success Criteria

The MVP is successful when:

- several Java or Spring-related Markdown documents can be uploaded;
- their ingestion state can be inspected;
- the documents can be browsed and searched;
- a technical question can be answered using retrieved document passages;
- the user can inspect the exact passages and source locations used;
- the implementation provides a clear learning path through Java, Spring Boot,
  vector retrieval, and RAG.

## 12. Explicitly Deferred

- Authentication and authorization.
- Deployment to a shared or public environment.
- Multi-user data separation.
- Importing documents directly from URLs.
- Crawling entire documentation websites.
- Repository synchronization.
- Embedded image and attachment processing.
- Conversational memory and follow-up questions.
- Multiple model providers.
- A dedicated vector database.
- Extensive frontend styling.
- Premature distributed or microservice architecture.

## 13. Open Decisions

These decisions should be made shortly before their implementation is required:

- Java, Spring Boot, Vue, and supporting library versions.
- Build tooling and repository layout.
- Markdown parser and exact parsing rules.
- Chunking strategy and chunk metadata.
- Embedding model and vector dimensionality.
- Similarity metric and indexing strategy.
- The shape of citations and the RAG debug interface.
- OpenAI model selection, token limits, and cost controls.
- Secrets and local configuration management.
