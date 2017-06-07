package com.novoda.pianohero;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import java.util.List;

public class C4ToB5TrebleStaffWidget extends FrameLayout {

    private static final int VIEW_HEIGHT_IN_NUMBER_OF_NOTES = 8;

    private final int trebleClefMarginLeftPx;
    private final Drawable trebleClefDrawable;
    private final Drawable completedNoteDrawable;
    private final Drawable completedSharpDrawable;
    private final Drawable noteDrawable;
    private final Drawable sharpDrawable;
    private final Drawable errorNoteDrawable;
    private final Drawable errorSharpDrawable;
    private final C4ToB5TrebleStaffPositioner positioner;
    private final Paint linesPaint;

    public C4ToB5TrebleStaffWidget(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);

        trebleClefMarginLeftPx = context.getResources().getDimensionPixelSize(R.dimen.treble_clef_margin_left);
        trebleClefDrawable = trebleClefDrawable(context);
        completedNoteDrawable = noteDrawable(context, R.drawable.note_completed);
        completedSharpDrawable = sharpDrawable(context, R.drawable.sharp_completed);
        noteDrawable = noteDrawable(context, R.drawable.note);
        sharpDrawable = sharpDrawable(context, R.drawable.sharp);
        errorNoteDrawable = noteDrawable(context, R.drawable.note_error);
        errorSharpDrawable = sharpDrawable(context, R.drawable.sharp_error);
        linesPaint = new Paint();
        linesPaint.setColor(ContextCompat.getColor(context, R.color.notation_default));

        positioner = C4ToB5TrebleStaffPositioner.createPositionerGivenNoteHeight(noteDrawable.getBounds().height());
    }

    private Drawable trebleClefDrawable(Context context) {
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.treble_clef);
        int width = context.getResources().getDimensionPixelSize(R.dimen.treble_clef_width);
        int height = context.getResources().getDimensionPixelSize(R.dimen.treble_clef_height);
        drawable.setBounds(0, 0, width, height);
        return drawable;
    }

    private Drawable noteDrawable(Context context, @DrawableRes int res) {
        Drawable drawable = ContextCompat.getDrawable(context, res);
        int width = context.getResources().getDimensionPixelSize(R.dimen.note_width);
        int height = context.getResources().getDimensionPixelSize(R.dimen.note_height);
        drawable.setBounds(0, 0, width, height);
        return drawable;
    }

    private Drawable sharpDrawable(Context context, @DrawableRes int res) {
        Drawable drawable = ContextCompat.getDrawable(context, res);
        int width = context.getResources().getDimensionPixelSize(R.dimen.sharp_width);
        int height = context.getResources().getDimensionPixelSize(R.dimen.sharp_height);
        drawable.setBounds(0, 0, width, height);
        return drawable;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = noteDrawable.getBounds().height() * VIEW_HEIGHT_IN_NUMBER_OF_NOTES;
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    public void showProgress(final RoundEndViewModel viewModel) {
        if (getMeasuredWidth() == 0) { // apparently onMeasure is first called after Activity.onResume, and we rely on onMeasure to layout correctly
            getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    showSubsetOfNotesThatFitFrom(viewModel);
                }
            });
        } else {
            showSubsetOfNotesThatFitFrom(viewModel);
        }
    }

    private void showSubsetOfNotesThatFitFrom(RoundEndViewModel viewModel) {
        final Sequence sequence = viewModel.getSequence();
        int spaceRequiredForTrebleClef = notesOffsetToAllowSpaceForTrebleClef();
        int eachNoteWidth = widthForNoteWidgetIncludingSharpAndLeftMargin();
        int numberOfNotesWeHaveSpaceFor = (getMeasuredWidth() - spaceRequiredForTrebleClef) / eachNoteWidth;

        int startIndexInclusive = (sequence.position() / numberOfNotesWeHaveSpaceFor) * numberOfNotesWeHaveSpaceFor;
        int endIndexExclusive = startIndexInclusive + numberOfNotesWeHaveSpaceFor;
        int windowStart = startIndexInclusive;
        int windowEnd = endIndexExclusive < sequence.length() ? endIndexExclusive : sequence.length();
        List<Note> currentWindow = sequence.notes().asList().subList(windowStart, windowEnd);
        int positionRelativeToWindow = sequence.position() - startIndexInclusive;

        show(currentWindow, positionRelativeToWindow);
    }

    private void show(List<Note> notes, int indexNextPlayableNote) {
        removeAllViews();

        for (int index = 0; index < notes.size(); index++) {
            Note note = notes.get(index);
            SequenceNote sequenceNote = new SequenceNote(note, index);
            boolean complete = index < indexNextPlayableNote;
            boolean sharp = note.isSharp();
            if (complete) {
                if (sharp) {
                    addSharpNoteWidget(sequenceNote, completedNoteDrawable, completedSharpDrawable);
                } else {
                    addNoteWidget(sequenceNote, completedNoteDrawable);
                }
            } else {
                if (sharp) {
                    addSharpNoteWidget(sequenceNote, noteDrawable, sharpDrawable);
                } else {
                    addNoteWidget(sequenceNote, noteDrawable);
                }
            }
        }
    }

    private void addNoteWidget(SequenceNote sequenceNote, Drawable noteDrawable) {
        NoteWidget noteWidget = new NoteWidget(getContext(), noteDrawable, null);
        noteWidget.setTag(R.id.tag_treble_staff_widget_note, sequenceNote);
        addView(noteWidget);
    }

    private void addSharpNoteWidget(SequenceNote sequenceNote, Drawable noteDrawable, Drawable sharpDrawable) {
        NoteWidget noteWidget = new NoteWidget(getContext(), noteDrawable, sharpDrawable);
        noteWidget.setTag(R.id.tag_treble_staff_widget_note, sequenceNote);
        addView(noteWidget);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        for (int i = 0; i < getChildCount(); i++) {
            layout((NoteWidget) getChildAt(i));
        }
    }

    private void layout(NoteWidget noteWidget) {
        SequenceNote sequenceNote = (SequenceNote) noteWidget.getTag(R.id.tag_treble_staff_widget_note);

        int noteLeft = notesOffsetToAllowSpaceForTrebleClef() + (sequenceNote.positionInSequence * widthForNoteWidgetIncludingSharpAndLeftMargin());
        if (noteWidget.getMeasuredWidth() <= noteDrawable.getBounds().width()) { // if this is not a sharped note
            noteLeft += sharpDrawable.getBounds().width(); // we leave a gap so spacing between notes (disregarding sharps) is uniform
        }

        int noteTop = (int) (positioner.yPosition(sequenceNote.note) - (0.5 * noteWidget.getMeasuredHeight()));
        int noteRight = noteLeft + noteWidget.getMeasuredWidth();
        int noteBottom = noteTop + noteWidget.getMeasuredHeight();
        noteWidget.layout(noteLeft, noteTop, noteRight, noteBottom);
    }

    private int widthForNoteWidgetIncludingSharpAndLeftMargin() {
        return sharpDrawable.getBounds().width() + noteDrawable.getBounds().width() + noteDrawable.getBounds().width();
    }

    private int notesOffsetToAllowSpaceForTrebleClef() {
        return trebleClefMarginLeftPx + (int) (trebleClefDrawable.getBounds().width() * 1.5);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int noteHeight = noteDrawable.getBounds().height();

        int saveCount = canvas.save();
        canvas.translate(trebleClefMarginLeftPx, (float) (noteHeight * 0.5));
        trebleClefDrawable.draw(canvas);
        canvas.restoreToCount(saveCount);

        int topStaffY = 2 * noteHeight;
        int bottomStaffY = topStaffY + 4 * noteHeight;
        drawStaffLines(canvas, noteHeight, topStaffY, bottomStaffY);
        drawLedgerLines(canvas, noteHeight, topStaffY, bottomStaffY);
    }

    private void drawStaffLines(Canvas canvas, int noteHeight, int topStaffY, int bottomStaffY) {
        for (int i = 0; i <= (bottomStaffY - topStaffY) / noteHeight; i++) {
            drawStaffLine(canvas, topStaffY + i * noteHeight);
        }
    }

    private void drawStaffLine(Canvas canvas, int y) {
        canvas.drawLine(0, y, getRight(), y, linesPaint);
    }

    private void drawLedgerLines(Canvas canvas, int noteHeight, int topStaffY, int bottomStaffY) {
        for (int i = 0; i < getChildCount(); i++) {
            NoteWidget noteWidget = (NoteWidget) getChildAt(i);
            Note note = ((SequenceNote) noteWidget.getTag(R.id.tag_treble_staff_widget_note)).note;
            if (Note.B5.equals(note) || Note.A5.equals(note) || Note.A5_S.equals(note)) {
                drawLedgerLineWithABitExtraEitherSide(canvas, noteWidget, topStaffY - noteHeight);
            } else if (Note.C4.equals(note) || Note.C4_S.equals(note)) {
                drawLedgerLineWithABitExtraEitherSide(canvas, noteWidget, bottomStaffY + noteHeight);
            }
        }
    }

    private void drawLedgerLineWithABitExtraEitherSide(Canvas canvas, NoteWidget noteWidget, int y) {
        int extra = (int) (noteWidget.getWidth() * 0.3);
        canvas.drawLine(noteWidget.getLeft() - extra, y, noteWidget.getRight() + extra, y, linesPaint);
    }

    public void showError(RoundEndViewModel viewModel) {
        Sequence sequence = viewModel.getSequence();
        int positionRelativeToWindow = getPositionRelativeToWindow(sequence.position());

        SequenceNote sequenceNote = new SequenceNote(sequence.latestError(), positionRelativeToWindow);
        addNoteWidget(sequenceNote, errorNoteDrawable);
    }

    public void showSharpError(RoundEndViewModel viewModel) {
        Sequence sequence = viewModel.getSequence();
        int positionRelativeToWindow = getPositionRelativeToWindow(sequence.position());

        SequenceNote sequenceNote = new SequenceNote(sequence.latestError(), positionRelativeToWindow);
        addSharpNoteWidget(sequenceNote, errorNoteDrawable, errorSharpDrawable);
    }

    private int getPositionRelativeToWindow(int currentPosition) {
        int spaceRequiredForTrebleClef = notesOffsetToAllowSpaceForTrebleClef();
        int eachNoteWidth = widthForNoteWidgetIncludingSharpAndLeftMargin();
        int numberOfNotesWeHaveSpaceFor = (getMeasuredWidth() - spaceRequiredForTrebleClef) / eachNoteWidth;

        int startIndexInclusive = (currentPosition / numberOfNotesWeHaveSpaceFor) * numberOfNotesWeHaveSpaceFor;
        return currentPosition - startIndexInclusive;
    }

    private static class SequenceNote {
        final Note note;
        final int positionInSequence;

        SequenceNote(Note note, int positionInSequence) {
            this.note = note;
            this.positionInSequence = positionInSequence;
        }
    }

}
