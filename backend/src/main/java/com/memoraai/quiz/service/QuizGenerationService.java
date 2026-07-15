package com.memoraai.quiz.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoraai.anme.dto.UserMemoryStateDto;
import com.memoraai.anme.entity.Concept;
import com.memoraai.anme.entity.UserMemoryState;
import com.memoraai.anme.mapper.ANMEMapper;
import com.memoraai.anme.repository.ConceptRepository;
import com.memoraai.anme.repository.UserMemoryStateRepository;
import com.memoraai.anme.service.ANMEMemoryService;
import com.memoraai.chat.service.LLMService;
import com.memoraai.chunking.entity.DocumentChunk;
import com.memoraai.chunking.repository.DocumentChunkRepository;
import com.memoraai.common.exception.ResourceNotFoundException;
import com.memoraai.document.entity.Document;
import com.memoraai.document.repository.DocumentRepository;
import com.memoraai.quiz.dto.AnswerDto;
import com.memoraai.quiz.dto.QuizDto;
import com.memoraai.quiz.dto.QuizQuestionDto;
import com.memoraai.quiz.dto.QuestionResultDto;
import com.memoraai.quiz.dto.QuizResultDto;
import com.memoraai.quiz.dto.SubmitQuizRequest;
import com.memoraai.quiz.entity.Quiz;
import com.memoraai.quiz.entity.QuizAttempt;
import com.memoraai.quiz.entity.QuizQuestion;
import com.memoraai.quiz.entity.QuizStatus;
import com.memoraai.quiz.entity.QuestionType;
import com.memoraai.quiz.repository.QuizAttemptRepository;
import com.memoraai.quiz.repository.QuizQuestionRepository;
import com.memoraai.quiz.repository.QuizRepository;
import com.memoraai.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Generates quizzes using extracted Concepts and Document Chunks via the LLM.
 * Grades submissions and updates UserMemoryState via ANMEMemoryService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuizGenerationService {

    private static final int DEFAULT_QUESTION_COUNT = 10;
    private static final int MAX_QUESTION_COUNT = 20;

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final QuizAttemptRepository attemptRepository;
    private final DocumentRepository documentRepository;
    private final ConceptRepository conceptRepository;
    private final DocumentChunkRepository chunkRepository;
    private final UserMemoryStateRepository memoryStateRepository;
    private final ANMEMemoryService memoryService;
    private final ANMEMapper anmeMapper;
    private final LLMService llmService;
    private final ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // Generate Quiz
    // -------------------------------------------------------------------------

    @Transactional
    public QuizDto generateQuiz(UUID documentId, User user, Integer requestedCount) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));

        int count = (requestedCount != null && requestedCount > 0)
                ? Math.min(requestedCount, MAX_QUESTION_COUNT)
                : DEFAULT_QUESTION_COUNT;

        List<Concept> concepts = conceptRepository.findByDocumentId(documentId);
        List<DocumentChunk> chunks = chunkRepository
                .findByExtractedDocumentDocumentIdOrderByChunkIndexAsc(documentId);

        if (concepts.isEmpty() || chunks.isEmpty()) {
            throw new IllegalStateException(
                    "Document " + documentId + " has no concepts or chunks yet. " +
                    "Please wait for processing to complete.");
        }

        Quiz quiz = quizRepository.save(Quiz.builder()
                .document(document)
                .user(user)
                .title("Quiz: " + document.getOriginalFileName())
                .questionCount(count)
                .status(QuizStatus.PENDING)
                .build());

        List<QuizQuestion> questions = generateQuestionsWithLLM(quiz, concepts, chunks, count);
        quiz.setQuestionCount(questions.size());
        quizRepository.save(quiz);

        log.info("Generated quiz {} with {} questions for document {}", quiz.getId(), questions.size(), documentId);

        return toQuizDto(quiz, questions);
    }

    // -------------------------------------------------------------------------
    // Get Quiz
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public QuizDto getQuiz(UUID quizId, UUID userId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + quizId));

        if (!quiz.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Quiz not found: " + quizId);
        }

        List<QuizQuestion> questions = questionRepository.findByQuizId(quizId);
        return toQuizDto(quiz, questions);
    }

    // -------------------------------------------------------------------------
    // Submit Quiz
    // -------------------------------------------------------------------------

    @Transactional
    public QuizResultDto submitQuiz(UUID quizId, User user, SubmitQuizRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + quizId));

        if (!quiz.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Quiz not found: " + quizId);
        }

        List<QuizQuestion> questions = questionRepository.findByQuizId(quizId);
        Map<UUID, QuizQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(QuizQuestion::getId, q -> q));

        int correct = 0;
        int wrong = 0;
        Map<String, String> answersMap = new HashMap<>();
        List<QuestionResultDto> questionResults = new ArrayList<>();

        // Track which concepts were tested
        Map<UUID, List<Boolean>> conceptResults = new HashMap<>();

        for (AnswerDto answer : request.getAnswers()) {
            if (answer.getQuestionId() == null) continue;

            QuizQuestion question = questionMap.get(answer.getQuestionId());
            if (question == null) continue;

            answersMap.put(answer.getQuestionId().toString(), answer.getUserAnswer());

            boolean isCorrect = question.getCorrectAnswer()
                    .equalsIgnoreCase(answer.getUserAnswer() != null ? answer.getUserAnswer().trim() : "");

            if (isCorrect) {
                correct++;
            } else {
                wrong++;
            }

            questionResults.add(QuestionResultDto.builder()
                    .questionId(question.getId())
                    .questionText(question.getQuestionText())
                    .userAnswer(answer.getUserAnswer())
                    .correctAnswer(question.getCorrectAnswer())
                    .isCorrect(isCorrect)
                    .build());

            // Track per-concept correctness
            if (question.getConcept() != null) {
                conceptResults
                    .computeIfAbsent(question.getConcept().getId(), k -> new ArrayList<>())
                    .add(isCorrect);
            }
        }

        int total = correct + wrong;
        double percentage = total > 0 ? ((double) correct / total) * 100.0 : 0.0;

        // Save attempt
        QuizAttempt attempt = QuizAttempt.builder()
                .quiz(quiz)
                .user(user)
                .answers(answersMap)
                .score(correct)
                .correctAnswers(correct)
                .wrongAnswers(wrong)
                .percentage(percentage)
                .completedAt(LocalDateTime.now())
                .build();
        attemptRepository.save(attempt);

        // Mark quiz completed
        quiz.setStatus(QuizStatus.COMPLETED);
        quizRepository.save(quiz);

        // Update memory states per concept
        List<UserMemoryStateDto> updatedMemory = updateMemoryForConcepts(user, conceptResults, percentage);

        return QuizResultDto.builder()
                .score(correct)
                .correctAnswers(correct)
                .wrongAnswers(wrong)
                .percentage(percentage)
                .updatedMemory(updatedMemory)
                .questionResults(questionResults)
                .build();
    }

    // -------------------------------------------------------------------------
    // LLM Question Generation
    // -------------------------------------------------------------------------

    private List<QuizQuestion> generateQuestionsWithLLM(
            Quiz quiz, List<Concept> concepts, List<DocumentChunk> chunks, int count) {

        // Build context: top concepts + sample chunks
        StringBuilder conceptContext = new StringBuilder();
        List<Concept> topConcepts = concepts.stream()
                .sorted((a, b) -> Double.compare(b.getImportanceScore(), a.getImportanceScore()))
                .limit(15)
                .collect(Collectors.toList());

        for (Concept c : topConcepts) {
            conceptContext.append("- ").append(c.getName()).append(": ").append(c.getDescription()).append("\n");
        }

        StringBuilder chunkContext = new StringBuilder();
        int chunkLimit = Math.min(chunks.size(), 5);
        for (int i = 0; i < chunkLimit; i++) {
            chunkContext.append(chunks.get(i).getChunkText()).append("\n\n");
        }

        String prompt = buildQuizPrompt(conceptContext.toString(), chunkContext.toString(), count);

        try {
            String json = llmService.generateAnswer(prompt).block();
            if (json == null) {
                log.warn("LLM returned null for quiz generation");
                return Collections.emptyList();
            }

            json = cleanJson(json);
            List<Map<String, Object>> rawQuestions = objectMapper.readValue(
                    json, new TypeReference<List<Map<String, Object>>>() {});

            List<QuizQuestion> saved = new ArrayList<>();
            Map<String, Concept> conceptNameMap = topConcepts.stream()
                    .collect(Collectors.toMap(
                            c -> c.getName().trim().toLowerCase(),
                            c -> c,
                            (a, b) -> a));

            for (Map<String, Object> raw : rawQuestions) {
                QuizQuestion q = parseQuestion(raw, quiz, conceptNameMap);
                if (q != null) {
                    saved.add(questionRepository.save(q));
                }
                if (saved.size() >= count) break;
            }

            return saved;
        } catch (Exception e) {
            log.error("Failed to parse quiz questions from LLM: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private QuizQuestion parseQuestion(Map<String, Object> raw, Quiz quiz, Map<String, Concept> conceptMap) {
        try {
            String questionText = (String) raw.get("question");
            String typeStr = (String) raw.get("type");
            String correctAnswer = (String) raw.get("correctAnswer");

            if (questionText == null || typeStr == null || correctAnswer == null) return null;

            QuestionType type;
            try {
                type = QuestionType.valueOf(typeStr.toUpperCase().replace(" ", "_"));
            } catch (IllegalArgumentException e) {
                return null;
            }

            @SuppressWarnings("unchecked")
            List<String> options = raw.containsKey("options")
                    ? (List<String>) raw.get("options")
                    : Collections.emptyList();

            // Map to concept if mentioned
            String conceptName = (String) raw.get("conceptName");
            Concept linkedConcept = conceptName != null
                    ? conceptMap.get(conceptName.trim().toLowerCase())
                    : null;

            double difficulty = 0.5;
            if (raw.containsKey("difficulty")) {
                Object d = raw.get("difficulty");
                if (d instanceof Number) {
                    difficulty = ((Number) d).doubleValue();
                }
            }

            return QuizQuestion.builder()
                    .quiz(quiz)
                    .concept(linkedConcept)
                    .questionType(type)
                    .questionText(questionText)
                    .options(options)
                    .correctAnswer(correctAnswer)
                    .difficulty(Math.min(1.0, Math.max(0.0, difficulty)))
                    .build();
        } catch (Exception e) {
            log.warn("Skipping malformed question: {}", e.getMessage());
            return null;
        }
    }

    private String buildQuizPrompt(String concepts, String chunks, int count) {
        return "You are an educational quiz generator for a student learning platform.\n" +
               "Generate exactly " + count + " quiz questions based on the concepts and text below.\n\n" +
               "Rules:\n" +
               "- Mix the following question types: MULTIPLE_CHOICE, TRUE_FALSE, FILL_BLANK\n" +
               "- For MULTIPLE_CHOICE: provide exactly 4 options\n" +
               "- For TRUE_FALSE: options must be [\"True\", \"False\"]\n" +
               "- For FILL_BLANK: options must be an empty list []\n" +
               "- correctAnswer must match one of the options exactly (case-sensitive)\n" +
               "- difficulty is a float from 0.0 (easy) to 1.0 (hard)\n" +
               "- conceptName should match one of the listed concept names\n\n" +
               "Return ONLY valid JSON. Do not include Markdown. Do not include any other text.\n" +
               "Format:\n" +
               "[\n" +
               "  {\n" +
               "    \"question\": \"What is...\",\n" +
               "    \"type\": \"MULTIPLE_CHOICE\",\n" +
               "    \"options\": [\"A\", \"B\", \"C\", \"D\"],\n" +
               "    \"correctAnswer\": \"A\",\n" +
               "    \"difficulty\": 0.4,\n" +
               "    \"conceptName\": \"Concept Name\"\n" +
               "  }\n" +
               "]\n\n" +
               "Concepts:\n" + concepts + "\n" +
               "Document Text:\n" + chunks;
    }

    // -------------------------------------------------------------------------
    // Memory Update
    // -------------------------------------------------------------------------

    private List<UserMemoryStateDto> updateMemoryForConcepts(
            User user, Map<UUID, List<Boolean>> conceptResults, double overallPercentage) {

        List<UserMemoryStateDto> updatedMemory = new ArrayList<>();

        for (Map.Entry<UUID, List<Boolean>> entry : conceptResults.entrySet()) {
            UUID conceptId = entry.getKey();
            List<Boolean> results = entry.getValue();

            long correctCount = results.stream().filter(Boolean::booleanValue).count();
            double conceptPercentage = (double) correctCount / results.size() * 100.0;

            Optional<UserMemoryState> stateOpt =
                    memoryStateRepository.findByUserIdAndConceptId(user.getId(), conceptId);

            if (stateOpt.isPresent()) {
                UserMemoryState updated = memoryService.updateMemoryAfterQuiz(stateOpt.get(), conceptPercentage);
                updatedMemory.add(anmeMapper.userMemoryStateToDto(updated));
            }
        }

        // If no concept-specific states were found, update memory globally with overall percentage
        if (updatedMemory.isEmpty()) {
            List<UserMemoryState> allStates = memoryStateRepository.findByUserIdAndConceptDocumentIsDeletedFalse(user.getId());
            for (UserMemoryState state : allStates.stream().limit(5).collect(Collectors.toList())) {
                UserMemoryState updated = memoryService.updateMemoryAfterQuiz(state, overallPercentage);
                updatedMemory.add(anmeMapper.userMemoryStateToDto(updated));
            }
        }

        return updatedMemory;
    }

    // -------------------------------------------------------------------------
    // Mapping Helpers
    // -------------------------------------------------------------------------

    private QuizDto toQuizDto(Quiz quiz, List<QuizQuestion> questions) {
        List<QuizQuestionDto> questionDtos = questions.stream()
                .map(this::toQuestionDto)
                .collect(Collectors.toList());

        return QuizDto.builder()
                .id(quiz.getId())
                .documentId(quiz.getDocument().getId())
                .title(quiz.getTitle())
                .questionCount(quiz.getQuestionCount())
                .status(quiz.getStatus())
                .createdAt(quiz.getCreatedAt())
                .questions(questionDtos)
                .build();
    }

    private QuizQuestionDto toQuestionDto(QuizQuestion q) {
        return QuizQuestionDto.builder()
                .id(q.getId())
                .questionType(q.getQuestionType())
                .questionText(q.getQuestionText())
                .options(q.getOptions())
                .conceptId(q.getConcept() != null ? q.getConcept().getId() : null)
                .difficulty(q.getDifficulty())
                .build();
    }

    private String cleanJson(String raw) {
        raw = raw.trim();
        if (raw.startsWith("```json")) raw = raw.substring(7);
        else if (raw.startsWith("```")) raw = raw.substring(3);
        if (raw.endsWith("```")) raw = raw.substring(0, raw.length() - 3);
        return raw.trim();
    }
}
