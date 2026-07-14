import fitz
doc = fitz.open()
page = doc.new_page()
page.insert_text((72, 72), "Milestone 4.2 PDF Extraction Test")
page.insert_text((72, 100), "This is a real PDF document to test the PyMuPDF extraction engine.")
doc.save("test_upload.pdf")
doc.close()
