# Changelog - Document Saathi

## [Recent Updates] - 2026-08-28

### ✨ New Features & Enhancements
- **Direct Navigation Flow**: Simplified the user journey. Now, after scanning a document, the app navigates directly from `ScannerFragment` to `DocumentDetailFragment`.
- **Improved PDF Export**: The "Save as PDF" feature now embeds the **actual document photo** instead of just text summaries, making it more useful for archiving.
- **Revamped Document Detail UI**:
    - **Extracted Info Cards**: Dedicated, clean cards for "Document ID" and "Holder Name".
    - **Icon-Only Actions**: Action buttons (Share, Delete, etc.) are now icon-only with increased breathing space for a more modern look.
- **Edge-to-Edge Support**: Fixed UI overlap issues where content was hidden behind the Status Bar and Navigation Bar using `WindowInsets`.

### 🛠 Architecture & Cleanup
- **Automatic Persistence**: Documents are now saved to the database immediately after OCR in the `ScannerViewModel`.
- **Codebase Sanitization**: Removed redundant fragments (`OcrResultFragment`, `SummarizationResultFragment`) and their associated ViewModels and XML layouts to keep the project lean.

### 📝 Documentation
- Created `plan.md` to track project roadmap.
- Updated `CHANGELOG.md` with latest milestones.
