# Implementation Plan - Finalize Delete Functionality

The goal is to connect the existing delete logic in the ViewModel to the UI, including a confirmation dialog for safety.

## User Review Required

> [!NOTE]
> I will implement a standard Material Alert Dialog for confirmation. This is best practice to prevent accidental deletions.

## Proposed Changes

### [Home Feature]

#### [MODIFY] [DocumentAdapter.kt](file:///D:/D/College Stuff/Sem-VII/MINI PROJECT/Document Saathi/app/src/main/java/com/rohan/documentsaathi/feature/home/ui/DocumentAdapter.kt)
- Add `onDeleteClick: (Document) -> Unit` to the constructor.
- Update `btnDelete.setOnClickListener` to trigger the callback.

#### [MODIFY] [HomeFragment.kt](file:///D:/D/College Stuff/Sem-VII/MINI PROJECT/Document Saathi/app/src/main/java/com/rohan/documentsaathi/feature/home/ui/HomeFragment.kt)
- Update `setupRecyclerView` to provide the delete callback.
- Add `showDeleteConfirmation(document: Document)` method.

## Verification Plan

### Manual Verification
- Deploy to device.
- Tap "Delete" on any document.
- Verify the confirmation dialog appears.
- Confirm deletion and ensure the document disappears from the list.
- Verify "Cancel" keeps the document.
