package com.unigram.reporting;

/**
 * High-level orchestration service for Phase 8 syllabus demonstrations:
 * 1. Functional Interfaces
 * 2. Lambda Expressions
 * 3. Method References
 * 4. Stream API
 * 5. Optional
 * 6. Generics
 * 7. Date and Time API
 * 8. File Handling
 * 9. CSV Export and Import
 */
public class Phase8SyllabusService {

    public void demonstrateFunctionalInterfaces() {
        FunctionalInterfaceDemo.runDemonstration();
    }

    public void demonstrateLambdaExpressions() {
        LambdaExpressionDemo.runDemonstration();
    }

    public void demonstrateMethodReferences() {
        MethodReferenceDemo.runDemonstration();
    }

    public void demonstrateStreamApi() {
        StreamApiDemo.runDemonstration();
    }

    public void demonstrateOptional() {
        OptionalDemo.runDemonstration();
    }

    public void demonstrateGenerics() {
        GenericsDemo.runDemonstration();
    }

    public void demonstrateDateTime() {
        DateTimeDemo.runDemonstration();
    }

    public void demonstrateFileHandling() {
        FileHandlingDemo.runDemonstration();
    }

    public void demonstrateCsvExportAndImport() {
        CsvExportDemo.runDemonstration();
    }

    public void runAllDemonstrations() {
        demonstrateFunctionalInterfaces();
        demonstrateLambdaExpressions();
        demonstrateMethodReferences();
        demonstrateStreamApi();
        demonstrateOptional();
        demonstrateGenerics();
        demonstrateDateTime();
        demonstrateFileHandling();
        demonstrateCsvExportAndImport();
    }
}
