# Implementation Plan - PDF Modification & Document Detail UI Update

This plan covers modifying the PDF save feature to include the document photo instead of the AI summary and updating the Document Detail screen according to the future plan.

## Proposed Changes

### [Summarization]

#### [MODIFY] [SummarizationResultFragment.kt](file:///D:/D/College Stuff/Sem-VII/MINI PROJECT/document_saathi/app/src/main/java/com/rohan/documentsaathi/feature/summarization/SummarizationResultFragment.kt)
- Update `savePdfDocument` to:
    - Load the bitmap from `args.documentImagePath`.
    - Draw the image onto the PDF canvas.
    - Remove the code that iterates through `fields` to draw summary text.
    - Keep basic document metadata (Type, Title).

### [Document Detail]

#### [MODIFY] [fragment_document_detail.xml](file:///D:/D/College Stuff/Sem-VII/MINI PROJECT/document_saathi/app/src/main/res/layout/fragment_document_detail.xml)
- Update **Document Metadata Card**:
    - Ensure Document Title and Scan Date are clearly displayed.
- Update **Extracted Text Section**:
    - Rename/Refactor to "Extracted Info".
    - Add fields for **Document ID** with a copy button.
    - Add fields for **Name of document holder**.
- **Remove Summary Section**:
    - Delete the summary card, header, and re-summarize button.
- **Actions Section**:
    - Modify the four buttons (`btn_rescan`, `btn_bookmark`, `btn_share_document`, `btn_delete`).
    - Remove `android:text` from buttons.
    - Use icons only.
    - Add margins/padding for "breathing space".

#### [MODIFY] [DocumentDetailFragment.kt](file:///D:/D/College Stuff/Sem-VII/MINI PROJECT/document_saathi/app/src/main/java/com/rohan/documentsaathi/feature/document/ui/DocumentDetailFragment.kt)
- Remove summary-related binding logic.
- Implement logic to bind Document ID and Name to the new UI elements (if data is available in the `Document` entity, otherwise use placeholders).
- Add click listener for the new Document ID copy button.

## Verification Plan

### Automated Tests
- N/A (UI and PDF generation are primarily verified manually).

### Manual Verification
1.  **PDF Save**:
    - Run the app, take a scan, go to summarization result.
    - Click "Save as PDF".
    - Verify the generated PDF contains the captured image instead of the summary text.
2.  **Document Detail UI**:
    - Navigate to a document's detail view.
    - Verify the layout matches the new design (Title, Date, Extracted Info with ID copy, no summary).
    - Verify Action buttons are icon-only and have more space.
