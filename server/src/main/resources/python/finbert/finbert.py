# noinspection PyUnresolvedReferences
from fastapi import FastAPI, Body
# noinspection PyUnresolvedReferences
from transformers import pipeline

app = FastAPI()
sentiment = pipeline("sentiment-analysis", model="ProsusAI/finbert")


@app.post("/analyze")
async def analyze(text: str = Body(..., media_type="text/plain")):
    result = sentiment(text)[0]
    return {"label": result["label"], "score": result["score"]}
