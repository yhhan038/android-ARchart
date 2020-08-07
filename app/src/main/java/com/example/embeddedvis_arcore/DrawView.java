package com.example.embeddedvis_arcore;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class DrawView extends View {
    Paint paint = new Paint();

    public float startX = 0;
    public float startY = 0;
    public float width = 1;
    public float height = 1;

    public DrawView(Context context) {
        super(context);
    }

    public void setPosition(float x, float y, float w, float h) {
        startX = x;
        startY = y;
        width = w;
        height = h;
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        paint.setColor(Color.RED);
        paint.setStrokeWidth(10);
        DrawBox(canvas, startX, startY, width, height);
    }

    public void DrawBox(Canvas canvas, float startX, float startY, float width, float height) {
        canvas.drawLine(startX, startY, startX + width, startY, paint);
        canvas.drawLine(startX, startY, startX, startY + height, paint);
        canvas.drawLine(startX + width, startY, startX + width, startY + height, paint);
        canvas.drawLine(startX, startY + height, startX + width, startY + height, paint);
    }

}
