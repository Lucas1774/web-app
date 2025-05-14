FROM python:3.9-slim
RUN pip install fastapi uvicorn transformers torch
COPY finbert.py /app/finbert.py
WORKDIR /app
CMD ["uvicorn", "finbert:app", "--host", "0.0.0.0", "--port", "80"]
