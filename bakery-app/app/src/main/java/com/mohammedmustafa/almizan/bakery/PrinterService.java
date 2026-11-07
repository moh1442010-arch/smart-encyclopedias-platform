package com.mohammedmustafa.almizan.bakery;

import android.app.Activity;
import android.print.*;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import java.io.FileOutputStream;

/** Offline printing through Android's installed print services. */
public final class PrinterService {
    private PrinterService() {}
    public static void print(Activity activity,String businessName,String jobName,String body) {
        PrintManager manager=(PrintManager)activity.getSystemService(Activity.PRINT_SERVICE);
        if(manager==null)throw new IllegalStateException("خدمة الطباعة غير متاحة");
        final PrintAttributes attributes=new PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setResolution(new PrintAttributes.Resolution("almizan","AlMizan",300,300))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS).build();
        final TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(12f);paint.setColor(0xFF111827);
        final String all=(businessName==null?"":businessName+"\n")+jobName+"\n\n"+(body==null?"":body);
        final int contentWidth=523,contentHeight=770;
        final StaticLayout layout=StaticLayout.Builder.obtain(all,0,all.length(),paint,contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
            .setTextDirection(android.text.TextDirectionHeuristics.RTL)
            .setLineSpacing(4f,1f).build();
        final int pages=Math.max(1,(layout.getHeight()+contentHeight-1)/contentHeight);
        manager.print(jobName,new PrintDocumentAdapter(){
            @Override public void onLayout(PrintAttributes oldA,PrintAttributes newA,CancellationSignal signal,LayoutResultCallback callback,Bundle extras){
                if(signal.isCanceled()){callback.onLayoutCancelled();return;}
                callback.onLayoutFinished(new PrintDocumentInfo.Builder(jobName+".pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(pages).build(),true);
            }
            @Override public void onWrite(PageRange[] requested,ParcelFileDescriptor destination,CancellationSignal signal,WriteResultCallback callback){
                PdfDocument pdf=new PdfDocument();
                try{
                    for(int pageNo=0;pageNo<pages;pageNo++){
                        boolean requestedPage=false;
                        for(PageRange range:requested)if(pageNo>=range.getStart()&&pageNo<=range.getEnd()){requestedPage=true;break;}
                        if(!requestedPage)continue;
                        if(signal.isCanceled()){callback.onWriteCancelled();return;}
                        PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo+1).create());
                        Canvas canvas=page.getCanvas();canvas.save();canvas.translate(36,36-pageNo*contentHeight);
                        layout.draw(canvas);canvas.restore();pdf.finishPage(page);
                    }
                    pdf.writeTo(new FileOutputStream(destination.getFileDescriptor()));
                    callback.onWriteFinished(requested);
                }catch(Exception e){callback.onWriteFailed(e.getMessage());}
                finally{pdf.close();try{destination.close();}catch(Exception ignored){}}
            }
        },attributes);
    }
}
