CREATE TABLE IF NOT EXISTS recommendation_news (
  recommendation_id INTEGER NOT NULL,
  news_id INTEGER NOT NULL,
  PRIMARY KEY (recommendation_id, news_id),
  CONSTRAINT fk_recommendation_news_recommendation
    FOREIGN KEY (recommendation_id)   REFERENCES recommendation(id) ON DELETE CASCADE,
  CONSTRAINT fk_recommendation_news_news
    FOREIGN KEY (news_id) REFERENCES news(id) ON DELETE CASCADE
);
