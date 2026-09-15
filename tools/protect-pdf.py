#!/usr/bin/env python3
"""Build a personalized, AES-256 protected buyer PDF from the private master PDF.

The master PDF must never be committed to the public repository.
Each buyer receives a different open password and visible buyer/license watermark.
"""
import argparse
import secrets
from io import BytesIO
from pathlib import Path
from pypdf import PdfReader, PdfWriter
from reportlab.pdfgen import canvas

PRODUCT_TITLE = "الموسوعة الشاملة في الذكاء الاصطناعي باللغة العربية"
AUTHOR = "محمد مصطفى بابكر / MOHAMMED MUSTAFA BABIKER"


def make_watermark(width, height, buyer, license_id):
    buf = BytesIO()
    c = canvas.Canvas(buf, pagesize=(width, height))
    c.saveState()
    try:
        c.setFillAlpha(0.16)
    except Exception:
        pass
    c.setFont("Helvetica-Bold", 14)
    c.translate(width / 2, height / 2)
    c.rotate(32)
    c.drawCentredString(0, 22, "LICENSED COPY | PERSONAL USE ONLY")
    c.setFont("Helvetica", 11)
    c.drawCentredString(0, 4, f"Buyer: {buyer}")
    c.drawCentredString(0, -14, f"License: {license_id}")
    c.restoreState()
    c.save()
    buf.seek(0)
    return PdfReader(buf).pages[0]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("source", help="private master PDF")
    ap.add_argument("output", help="buyer PDF output")
    ap.add_argument("--buyer", required=True)
    ap.add_argument("--license", required=True)
    ap.add_argument("--password", required=True)
    ap.add_argument("--pages", type=int, default=250)
    args = ap.parse_args()

    if len(args.password) < 12:
        raise SystemExit("ERROR: buyer password must be at least 12 characters")
    if not 1 <= args.pages <= 250:
        raise SystemExit("ERROR: --pages must be between 1 and 250")

    source = Path(args.source)
    output = Path(args.output)
    reader = PdfReader(str(source))
    if len(reader.pages) < args.pages:
        raise SystemExit(f"ERROR: source has {len(reader.pages)} pages; requested {args.pages}")

    writer = PdfWriter()
    for i in range(args.pages):
        page = reader.pages[i]
        wm = make_watermark(float(page.mediabox.width), float(page.mediabox.height), args.buyer, args.license)
        page.merge_page(wm)
        writer.add_page(page)

    owner_password = secrets.token_urlsafe(36)
    writer.encrypt(args.password, owner_password, permissions_flag=0, algorithm="AES-256")
    writer.add_metadata({
        "/Title": f"{PRODUCT_TITLE} — نسخة مرخّصة",
        "/Author": AUTHOR,
        "/Subject": f"Licensed personal-use copy | {args.license}",
        "/Keywords": f"{args.license}, licensed copy, personal use",
        "/Buyer": args.buyer,
        "/License": args.license,
    })
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("wb") as fh:
        writer.write(fh)

    print(f"OUTPUT={output}")
    print(f"PAGES={args.pages}")
    print(f"LICENSE={args.license}")
    print("ENCRYPTION=AES-256")
    print("PERMISSIONS=NONE")
    print("OWNER_PASSWORD=GENERATED_AND_NOT_DISPLAYED")


if __name__ == "__main__":
    main()
