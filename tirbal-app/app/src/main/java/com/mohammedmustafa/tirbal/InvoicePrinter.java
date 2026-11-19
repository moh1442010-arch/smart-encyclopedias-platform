package com.mohammedmustafa.tirbal;

import android.app.Activity;
import android.content.Context;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.text.NumberFormat;

final class InvoicePrinter {
    private InvoicePrinter() {}

    static void print(Activity activity, String invoice, String date, long gross,
                      long allocatedCost, long marketExpenses, long net,
                      String trader, String project, NumberFormat money) {
        WebView web = new WebView(activity);
        web.getSettings().setJavaScriptEnabled(false);
        String html = buildHtml(invoice, date, gross, allocatedCost, marketExpenses,
                net, trader, project, money);
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                PrintManager manager = (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);
                if (manager == null) return;
                PrintAttributes attributes = new PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.UNKNOWN_PORTRAIT)
                        .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build();
                manager.print("فاتورة-" + safeFileName(invoice),
                        view.createPrintDocumentAdapter("فاتورة-" + safeFileName(invoice)),
                        attributes);
            }
        });
        web.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    private static String buildHtml(String invoice, String date, long gross,
                                    long allocatedCost, long marketExpenses, long net,
                                    String trader, String project, NumberFormat money) {
        return "<!doctype html><html lang='ar' dir='rtl'><head><meta charset='utf-8'>" +
                "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "<style>@page{size:80mm auto;margin:4mm}body{font-family:Tahoma,Arial,sans-serif;color:#111;width:100%;margin:0;font-size:12px}" +
                ".center{text-align:center}.brand{font-size:19px;font-weight:bold;color:#185b37}.muted{color:#555}" +
                "hr{border:0;border-top:1px dashed #777;margin:10px 0}.line{display:flex;justify-content:space-between;gap:8px;padding:5px 0;border-bottom:1px dotted #bbb}" +
                ".total{font-size:16px;font-weight:bold}.foot{text-align:center;margin-top:16px;font-size:11px}</style></head><body>" +
                "<div class='center brand'>حساب التربال</div><div class='center'>" + esc(project) + "</div>" +
                "<hr><div class='center'>فاتورة بيع</div>" +
                "<div class='line'><span>رقم الفاتورة</span><b>" + esc(invoice) + "</b></div>" +
                "<div class='line'><span>التاريخ</span><b>" + esc(date) + "</b></div>" +
                "<div class='line'><span>التاجر</span><b>" + esc(trader) + "</b></div><hr>" +
                row("قيمة البيع", money.format(gross)) +
                row("التكلفة المخصصة", money.format(allocatedCost)) +
                row("منصرفات السوق", money.format(marketExpenses)) +
                "<hr>" + "<div class='line total'><span>صافي الفاتورة</span><b>" + money.format(net) + " ج.س</b></div>" +
                "<hr><div class='foot'>تم إنشاء الفاتورة بواسطة حساب التربال<br>شكراً لتعاملكم</div></body></html>";
    }

    private static String row(String label, String value) {
        return "<div class='line'><span>" + label + "</span><b>" + value + " ج.س</b></div>";
    }

    private static String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String safeFileName(String value) {
        return value == null ? "جديدة" : value.replaceAll("[^A-Za-z0-9_-]", "_");
    }
}
