document.addEventListener("DOMContentLoaded", () => {
    // 1. فتح وإغلاق الصور (Lightbox)
    const previewButtons = document.querySelectorAll(".preview-card .page");
    
    previewButtons.forEach((btn, index) => {
        btn.addEventListener("click", () => {
            const img = btn.querySelector("img");
            if (img) {
                openLightbox(img.src, `صفحة معاينة رقم ${index + 1}`);
            }
        });
    });

    function openLightbox(src, title) {
        let box = document.getElementById("lightbox");
        if (!box) {
            box = document.createElement("div");
            box.id = "lightbox";
            box.className = "lightbox";
            box.innerHTML = `
                <button onclick="document.getElementById('lightbox').remove()">✕</button>
                <img src="" id="lightbox-img">
                <div id="lightbox-title"></div>
            `;
            document.body.appendChild(box);
        }
        document.getElementById("lightbox-img").src = src;
        document.getElementById("lightbox-title").innerText = title;
    }

    // 2. منطق شات البوت الذكي
    const chatForm = document.getElementById("chat-form");
    const chatInput = document.getElementById("chat-input");
    const messages = document.getElementById("chat-messages");

    if (chatForm) {
        chatForm.addEventListener("submit", (e) => {
            e.preventDefault();
            const text = chatInput.value.trim();
            if (!text) return;

            addMessage(text, "usermsg");
            chatInput.value = "";

            setTimeout(() => {
                respondToUser(text);
            }, 400);
        });
    }

    function addMessage(text, className) {
        if (!messages) return;
        const msg = document.createElement("div");
        msg.className = `msg ${className}`;
        msg.innerText = text;
        messages.appendChild(msg);
        messages.scrollTop = messages.scrollHeight;
    }

    function respondToUser(text) {
        const q = text.toLowerCase();
        let reply = "";

        // فحص الاستفسار عن عدد الصفحات
        if (q.includes("صفح") || q.includes("عدد") || q.includes("حجم")) {
            const total = (typeof CONFIG !== 'undefined' && CONFIG.fullPages) ? CONFIG.fullPages : 250;
            const preview = (typeof CONFIG !== 'undefined' && CONFIG.previewPages) ? CONFIG.previewPages : 20;
            reply = `يحتوي الكتاب الكامل على ${total} صفحة، وتتوفر منه ${preview} صفحة للمعاينة المجانية عبر الموقع.`;
        } 
        // فحص الاستفسار عن السعر
        else if (q.includes("سعر") || q.includes("بكم") || q.includes("تكلف")) {
            const price = (typeof CONFIG !== 'undefined' && CONFIG.bookPrice) ? CONFIG.bookPrice : "150";
            reply = `سعر النسخة الكاملة هو ${price} جنيه سوداني فقط.`;
        } 
        // فحص الاستفسار عن الدفع والتحويل
        else if (q.includes("دفع") || q.includes("شراء") || q.includes("بنك") || q.includes("تحويل")) {
            const bank = (typeof CONFIG !== 'undefined' && CONFIG.paymentBank) ? CONFIG.paymentBank : "بنكك";
            reply = `يمكنك التملّك والدفع المباشر عبر تطبيق (${bank}). بعد التحويل يرجى إرسال الإشعار للواتساب لاستلام النسخة فوراً.`;
        } 
        // إجابة عامة وترحيب
        else {
            reply = "أهلاً بك! أنا المساعد الذكي للموسوعة. يمكنك الاستفسار عن: عدد الصفحات، سعر الكتاب، أو طريقة الشراء والدفع عبر بنكك.";
        }

        addMessage(reply, "botmsg");
    }
});

