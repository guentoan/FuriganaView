package com.akira.nguyen.furigana.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;


import com.akira.nguyen.furigana.R;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Akira on 2016/06/24.
 */
public class FuriganaView extends TextView {
    private static final String TAG = FuriganaView.class.getSimpleName();
    private static final boolean DEBUG = true;
    private static final int TIME_LONG_CLICK = 1000;
    private static final int TYPE_NORMAL = 1;
    private static final int TYPE_BOLDER = 2;
    private static final int TYPE_ITALIC = 4;
    private static final int TYPE_FURIGANA = 8;
    /* regex for kanji with struct {kanji ; furigana} */
    private static final String KANJI_REGEX = "(([{])([\\u4E00-\\u9FFF\\u3040-\\u30FF0-9]*)([;])([\\u3040-\\u30FFー[0-9]]*)([}]))";
    private static final String BOLD_TEXT_REGEX = "(?m)(?d)(?s)(([<][b][>])(.*?)([<][\\/][b][>]))";
    private static final String ITALIC_TEXT_REGEX = "(?m)(?d)(?s)(([<][i][>])(.*?)([<][\\/][i][>]))";
    private static final String BREAK_REGEX = "(<br ?\\/?>)";
    private static final String BREAK_CHARACTER = "\n";

    private String mText;
    private Vector<Line> mLines;
    private Vector<PairText> mAllTexts;
    private TextPaint mNormalTextPaint;
    private TextPaint mFuriganaTextPaint;
    private TextPaint mBoldTextPaint;
    private TextPaint mItalicTextPaint;
    private TextPaint mBoldItalicTextPaint;
    private float mLineHeight;
    private float mMaxLineWidth;
    private float mLineSpacing;
    private float mTouchX;
    private float mTouchY;
    private boolean needToCallPerformOnClick = false;
    private OnTextSelectedListener mOnTextSelectedListener;

    private Runnable mLongClickRunnable = new Runnable() {
        @Override
        public void run() {
            performLongClick();
            needToCallPerformOnClick = false;
        }
    };

    public FuriganaView(Context context) {
        super(context);
        initialize(context, null);
    }

    public FuriganaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs);
    }

    public FuriganaView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs);
    }

    private void initialize(Context context, AttributeSet attrs) {
        mLines = new Vector<>();
        mAllTexts = new Vector<>();
        TextPaint textPaint = getPaint();
        mNormalTextPaint = new TextPaint(textPaint);

        mFuriganaTextPaint = new TextPaint(textPaint);
        mFuriganaTextPaint.setTextSize(textPaint.getTextSize()/2);

        mBoldTextPaint = new TextPaint(textPaint);
        mBoldTextPaint.setFakeBoldText(true);

        mItalicTextPaint = new TextPaint(textPaint);
        mItalicTextPaint.setTextSkewX(-0.35f);

        mBoldItalicTextPaint = new TextPaint(textPaint);
        mBoldItalicTextPaint.setTextSkewX(-0.35f);
        mBoldItalicTextPaint.setFakeBoldText(true);

        if(attrs == null) {
            mLineSpacing = 25;
        } else {
            TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.FuriganaView, 0, 0);
            try {
                mText = typedArray.getString(R.styleable.FuriganaView_jText);
                mLineSpacing = typedArray.getInteger(R.styleable.FuriganaView_line_spacing, 25);
            } finally {
                typedArray.recycle();
            }
        }

        // Calculate the height of one line.
        mLineHeight = mFuriganaTextPaint.getFontSpacing()
                + Math.max(Math.max(Math.max(mNormalTextPaint.getFontSpacing(),
                mBoldTextPaint.getFontSpacing()),
                mItalicTextPaint.getFontSpacing()),
                mBoldItalicTextPaint.getFontSpacing())
                + mLineSpacing;

        if(!TextUtils.isEmpty(mText)) {
            setJText();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if(widthMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.AT_MOST && widthMode > 0) {
            measureText(widthSize);
        } else {
            measureText(-1);
        }

        int height = (int) Math.round(Math.ceil(mLineHeight * (float) mLines.size())) + 15;
        int width = widthSize;
        if(widthMode != MeasureSpec.EXACTLY && mLines.size() <= 1) {
            width = (int) Math.round(Math.ceil(mMaxLineWidth));
        }
        if(heightMode != MeasureSpec.UNSPECIFIED && height > heightSize) {
            height |= MEASURED_STATE_TOO_SMALL;
        }

        setMeasuredDimension(width, height);
    }

    /***
     * Measure view with max width.
     * @param width if width < 0 → the view has one line, which has width unlimited.
     */
    private void measureText(int width) {
        mLines.clear();
        mMaxLineWidth = 0;

        if(width < 0) {
            Line line = new Line();
            line.mPairTexts = mAllTexts;
            for(int i = 0; i < mAllTexts.size(); i++) {
                mMaxLineWidth += mAllTexts.get(i).mWidth;
            }
        } else {
            float widthTemp = 0;
            Vector<PairText> pairTexts = new Vector<>();
            for(int i = 0; i < mAllTexts.size(); i++) {
                PairText pairText = mAllTexts.get(i);

                // Break to new line if {@PairText} contain break character.
                if(pairText.isBreak) {
                    Line line = new Line();
                    line.mPairTexts = pairTexts;
                    mLines.add(line);
                    mMaxLineWidth = mMaxLineWidth > widthTemp ? mMaxLineWidth : widthTemp;

                    // Reset for new line
                    pairTexts = new Vector<>();
                    widthTemp = 0;
                    continue;
                }

                widthTemp += pairText.mWidth;
                if(widthTemp < width) {
                    pairTexts.add(pairText);
                } else {
                    Line line = new Line();
                    widthTemp -= pairText.mWidth;
                    // If kanji -> break to new line
                    if(pairText.mFuriganaText != null) {
                        line.mPairTexts = pairTexts;
                        mLines.add(line);
                        mMaxLineWidth = mMaxLineWidth > widthTemp ? mMaxLineWidth : widthTemp;

                        // Reset for new line
                        pairTexts = new Vector<>();
                        pairTexts.add(pairText);
                        widthTemp = pairText.mWidth;
                    } else {
                        PairText splitPairText = pairText.split(widthTemp, width, pairTexts);

                        // Add new line
                        line.mPairTexts = pairTexts;
                        mLines.add(line);
                        mMaxLineWidth = mMaxLineWidth > widthTemp ? mMaxLineWidth : widthTemp;
                        if(splitPairText == null) {
                            pairTexts = new Vector<>();
                            continue;
                        }
                        // Reset for new line
                        widthTemp = splitPairText.mWidth;
                        //split for long text
                        while (widthTemp > width) {
                            widthTemp = 0;
                            pairTexts = new Vector<>();
                            splitPairText = splitPairText.split(widthTemp, width, pairTexts);
                            line = new Line();
                            line.mPairTexts = pairTexts;
                            mLines.add(line);
                            mMaxLineWidth = mMaxLineWidth > widthTemp ? mMaxLineWidth : widthTemp;

                            if(splitPairText != null) {
                                widthTemp = splitPairText.mWidth;
                            } else {
                                widthTemp = 0;
                            }
                        }

                        pairTexts = new Vector<>();
                        if(splitPairText != null) {
                            pairTexts.add(splitPairText);
                        }
                    }
                }

                // Make the last line before quit loop.
                if(i == (mAllTexts.size() - 1) && pairTexts.size() > 0) {
                    Line line = new Line();
                    line.mPairTexts = pairTexts;
                    mLines.add(line);
                    mMaxLineWidth = mMaxLineWidth > widthTemp ? mMaxLineWidth : widthTemp;
                }
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(mLines.size() > 0) {
            float y = mLineHeight;
            for(int i = 0; i < mLines.size(); i++) {
                Line line = mLines.get(i);
                Rect rect = new Rect(0, (int)(y - mLineHeight), (int)mMaxLineWidth, (int)y);
                line.mLineRect = rect;
                float x = 0;
                for(int j = 0; j < line.mPairTexts.size(); j++) {
                    PairText pairText = line.mPairTexts.get(j);
                    Rect pairRect = new Rect((int)x, (int)(y - mLineHeight), (int)(x + pairText.mWidth), (int)y);
                    pairText.mPairRect = pairRect;
                    pairText.onDraw(canvas, x, y);
                    x += pairText.mWidth;
                }

                y += mLineHeight;
            }
        }else {
            super.onDraw(canvas);
        }
    }

    public void resetText() {
        mLines.clear();
        mAllTexts.clear();
        invalidate();
        requestLayout();
    }


    /**
     * Sets the text that this View is to display.
     * @param text text to display
     */
    public void setJText(CharSequence text) {
        if(TextUtils.isEmpty(text)) {
            throw new IllegalArgumentException("input text is empty");
        }

        mText = (String) text;
        setJText();

        invalidate();
        requestLayout();
    }

    /**
     * Set text without invalidate
     */
    private void setJText() {
        mLines.clear();
        mText = mText.replaceAll(BREAK_REGEX, BREAK_CHARACTER);
        parseBoldText(mText);
    }

    /**
     * Parse text with struct <b>...</b>
     * @param text text to parse
     */
    private void parseBoldText(String text) {
        Pattern pattern = Pattern.compile(BOLD_TEXT_REGEX);
        Matcher matcher = pattern.matcher(text);
        int start = 0;
        int end;
        while(matcher.find()) {
            String fullText = matcher.group(1);
            String boldText = matcher.group(3);

            end = text.indexOf(fullText, start);
            if(end < 0) {
                continue;
            }

            if(end > start) {
                String normalText = text.substring(start, end);
                parseItalicText(normalText, TYPE_NORMAL);
            }

            parseItalicText(boldText, TYPE_BOLDER);
            start = end + fullText.length();
        }

        end = text.length();
        if(end > start) {
            String parseText = text.substring(start, end);
            parseItalicText(parseText, TYPE_NORMAL);
        }
    }

    /**
     * Parse text with struct <i>...</i>
     * @param text text to parse
     * @param type 2 type for this function. bold and normal.
     */
    private void parseItalicText(String text, int type) {
        Pattern pattern = Pattern.compile(ITALIC_TEXT_REGEX);
        Matcher matcher = pattern.matcher(text);
        int start = 0;
        int end;
        while(matcher.find()) {
            String fullText = matcher.group(1);
            String italicText = matcher.group(3);
            end = text.indexOf(fullText, start);
            if(end < 0) {
                continue;
            }

            if(end > start) {
                String normalText = text.substring(start, end);
                parseText(normalText, type);
            }

            parseText(italicText, type == TYPE_BOLDER ? (TYPE_BOLDER|TYPE_ITALIC) : TYPE_ITALIC);
            start = end + fullText.length();
        }

        end = text.length();
        if(end > start) {
            String parseText = text.substring(start, end);
            parseText(parseText, type);
        }
    }

    /**
     * Parse text with struct {kanji;furigana}
     * @param text text to parse
     * @param type 4 type for display. bold, italic, bold-italic and normal.
     */
    private void parseText(String text, int type) {
        Pattern pattern = Pattern.compile(KANJI_REGEX);
        Matcher matcher = pattern.matcher(text);
        int start = 0;
        int end;
        while(matcher.find()) {
            String fullText = matcher.group(1);
            String kanji = matcher.group(3);
            String furigana = matcher.group(5);

            end = text.indexOf(fullText, start);
            if(end < 0) {
                continue;
            }

            if(end > start) {
                String normalText = text.substring(start, end);
                parseBreakLineText(normalText, type);
            }

            JText kanjiText = new JText(kanji, type);
            FuriganaText furiganaText = new FuriganaText(furigana);
            PairText pairText = new PairText(kanjiText, furiganaText);
            mAllTexts.add(pairText);
            start = end + fullText.length();
        }

        end = text.length();
        if(end > start) {
            String normalText = text.substring(start, end);
            parseBreakLineText(normalText, type);
        }
    }

    /**
     * Parse text with struct \n \r <br> <br /> <br/>
     * @param text text to parse
     * @param type 4 type for display. bold, italic, bold-italic and normal.
     */
    private void parseBreakLineText(String text, int type) {
        if(text.contains(BREAK_CHARACTER)) {
            int breakIndex = text.indexOf(BREAK_CHARACTER);
            String firstText = text.substring(0, breakIndex);
            JText jText = new JText(firstText, type);
            PairText pairText = new PairText(jText);
            mAllTexts.add(pairText);

            PairText breakPairText = new PairText();
            mAllTexts.add(breakPairText);

            String secondText = text.substring(breakIndex+BREAK_CHARACTER.length());
            parseBreakLineText(secondText, type);
        } else {
            JText jText = new JText(text, type);
            PairText pairText = new PairText(jText);
            mAllTexts.add(pairText);
        }
    }

    @Override
    public boolean performClick() {
        handleClickText();
        return super.performClick();
    }

    @Override
    public boolean performLongClick() {
        handleClickText();
        return super.performLongClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()&MotionEvent.ACTION_MASK){
            case MotionEvent.ACTION_DOWN:
                needToCallPerformOnClick = true;
                mTouchX = event.getX();
                mTouchY = event.getY();
                postDelayed(mLongClickRunnable, TIME_LONG_CLICK);
                break;
            case MotionEvent.ACTION_UP:
                if(needToCallPerformOnClick) {
                    removeCallbacks(mLongClickRunnable);
                    mTouchX = event.getX();
                    mTouchY = event.getY();
                    performClick();
                }
                break;
            default:
                needToCallPerformOnClick = false;
                removeCallbacks(mLongClickRunnable);
                break;
        }

        super.onTouchEvent(event);
        return true;
    }

    private boolean handleClickText() {
        boolean result = false;
        for(int i = 0; i < mLines.size(); i++) {
            Line line = mLines.get(i);
            if(line.contain((int)mTouchX, (int)mTouchY)) {
                for(int j = 0; j < line.mPairTexts.size(); j++) {
                    PairText pairText = line.mPairTexts.get(j);
                    result = pairText.contain((int)mTouchX, (int)mTouchY);
                    if(result) {
                        String text = pairText.mJText.mText;
                        if(mOnTextSelectedListener != null) {
                            mOnTextSelectedListener.onTextSelected(text);
                        }
                        break;
                    }
                }
                break;
            }
        }

        return result;
    }

    public void setOnTextSelectedListener(OnTextSelectedListener onTextSelectedListener) {
        this.mOnTextSelectedListener = onTextSelectedListener;
    }

    private class Line {
        private Vector<PairText> mPairTexts;
        private Rect mLineRect;

        public boolean contain(int x, int y) {
            return mLineRect.contains(x, y);
        }
    }

    private class PairText {
        Rect mPairRect;
        JText mJText;
        FuriganaText mFuriganaText;
        float mWidth;
        boolean isBreak = false;

        public PairText() {
            isBreak = true;
        }

        public PairText(JText jText) {
            mJText = jText;
            measureWidth();
        }

        public PairText(JText jText, FuriganaText furiganaText) {
            mJText = jText;
            mFuriganaText = furiganaText;
            measureWidth();
        }

        private void measureWidth() {
            if(mFuriganaText == null) {
                mWidth = mJText.mWidth;
            } else {
                mWidth = Math.max(mJText.mWidth, mFuriganaText.mWidth);
            }
        }

        public PairText split(float width, float maxWidth, Vector<PairText> pairTexts) {
            if(mFuriganaText != null) {
                return null;
            }

            PairText pairText = mJText.split(width, maxWidth, pairTexts);
            return pairText;
        }

        public void onDraw(Canvas canvas, float x, float y) {
            if(mFuriganaText == null) {
                mJText.onDraw(canvas, x, y);
            } else {
                float normalX = x + (mWidth - mJText.mWidth)/2;
                mJText.onDraw(canvas, normalX, y);

                float furiganaX = x + (mWidth - mFuriganaText.mWidth)/2;
                float furiganaY = y - mJText.mHeight;
                mFuriganaText.onDraw(canvas, furiganaX, furiganaY);
            }
        }

        public boolean contain(int x, int y) {
            return mPairRect.contains(x, y);
        }
    }

    private class JText {
        String mText;
        int mType;
        float mWidth;
        float mHeight;
        float[] mWidthCharArray;
        TextPaint mTextPaint;

        public JText(String text, int type) {
            mText = text;
            mType = type;
            mWidthCharArray = new float[mText.length()];

            if(((mType & TYPE_FURIGANA) == TYPE_FURIGANA)) {
                mFuriganaTextPaint.getTextWidths(mText, mWidthCharArray);
                mHeight = mFuriganaTextPaint.descent() - mFuriganaTextPaint.ascent();
                mTextPaint = mFuriganaTextPaint;
            } else if(((mType & TYPE_BOLDER) == TYPE_BOLDER) && ((mType & TYPE_ITALIC) == TYPE_ITALIC)) {
                mBoldItalicTextPaint.getTextWidths(mText, mWidthCharArray);
                mHeight = mBoldItalicTextPaint.descent() - mBoldItalicTextPaint.ascent();
                mTextPaint = mBoldItalicTextPaint;
            } else if(((mType & TYPE_BOLDER) == TYPE_BOLDER)) {
                mBoldTextPaint.getTextWidths(mText, mWidthCharArray);
                mHeight = mBoldTextPaint.descent() - mBoldTextPaint.ascent();
                mTextPaint = mBoldTextPaint;
            } else if(((mType & TYPE_ITALIC) == TYPE_ITALIC)) {
                mItalicTextPaint.getTextWidths(mText, mWidthCharArray);
                mHeight = mItalicTextPaint.descent() - mItalicTextPaint.ascent();
                mTextPaint = mItalicTextPaint;
            } else if(((mType & TYPE_NORMAL) == TYPE_NORMAL)) {
                mNormalTextPaint.getTextWidths(mText, mWidthCharArray);
                mHeight = mNormalTextPaint.descent() - mNormalTextPaint.ascent();
                mTextPaint = mNormalTextPaint;
            }

            mWidth = 0;
            for (int i = 0; i < mWidthCharArray.length; i++) {
                mWidth += mWidthCharArray[i];
            }
        }

        public void onDraw(Canvas canvas, float x, float y) {
            mTextPaint.setColor(getCurrentTextColor());
            canvas.drawText(mText, 0, mText.length(), x, y, mTextPaint);
        }

        public PairText split(float width, float maxWidth, Vector<PairText> pairTexts) {
            int i = 0;
            for(; i < mWidthCharArray.length; i++) {
                width += mWidthCharArray[i];
                if(width < maxWidth) {
                    continue;
                } else {
                    width -= mWidthCharArray[i];
                    i--;
                    break;
                }
            }

            if(i <= 0) {
                return new PairText(new JText(mText, mType));
            } else {
                String newText = mText.substring(0, i);
                PairText pairText1 = new PairText(new JText(newText, mType));
                pairTexts.add(pairText1);
                if( i == mText.length()) {
                    return null;
                } else {
                    String newText1 = mText.substring(i);
                    PairText result = new PairText(new JText(newText1, mType));
                    return result;
                }
            }
        }
    }

    private class FuriganaText extends JText{
        public FuriganaText(String text) {
            super(text, TYPE_FURIGANA);
        }
    }

    public interface OnTextSelectedListener {
        void onTextSelected(String text);
    }
}

