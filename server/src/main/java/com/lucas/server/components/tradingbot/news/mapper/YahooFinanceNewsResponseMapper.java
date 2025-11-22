package com.lucas.server.components.tradingbot.news.mapper;

import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.utils.Mapper;
import com.lucas.utils.exception.MappingException;
import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

import static com.lucas.server.common.Constants.*;

@Component
public class YahooFinanceNewsResponseMapper implements Mapper<Element, News> {

    @Override
    public News map(Element item) throws MappingException {
        try {
            return new News()
                    .setExternalId((long) item.getElementsByTagName("guid").item(0).getTextContent().hashCode())
                    .setDate(LocalDateTime.from(ZonedDateTime.parse(
                            item.getElementsByTagName("pubDate").item(0).getTextContent(),
                            DateTimeFormatter.RFC_1123_DATE_TIME
                    )))
                    .setHeadline(item.getElementsByTagName("title").item(0).getTextContent())
                    .setSummary(StringUtils.left(item.getElementsByTagName("description").item(0).getTextContent(), 1024))
                    .setUrl(item.getElementsByTagName("link").item(0).getTextContent())
                    .setSource("Yahoo Finance RSS");
        } catch (Exception e) {
            throw new MappingException(MessageFormat.format(MAPPING_ERROR, NEWS), e);
        }
    }

    public Set<News> mapAll(Document document, Symbol symbol) throws MappingException {
        try {
            NodeList items = document.getElementsByTagName("item");
            if (null == items || 0 == items.getLength()) {
                throw new MappingException(MessageFormat.format(NO_YAHOO_NEWS_ERROR, document));
            }

            Set<News> newsList = new HashSet<>();
            for (int i = 0; i < items.getLength(); i++) {
                newsList.add(map((Element) items.item(i)).addSymbol(symbol));
            }
            return newsList;
        } catch (Exception e) {
            throw new MappingException(MessageFormat.format(MAPPING_ERROR, NEWS), e);
        }
    }
}
