package com.example.reviewservice.kafka.consumer;

import com.example.reviewservice.dto.BookDTO;
import com.example.reviewservice.dto.ReviewDTO;
import com.example.reviewservice.mapper.BookMapper;
import com.example.reviewservice.model.Book;
import com.example.reviewservice.model.BookOperationType;
import com.example.reviewservice.service.BookService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumer {
    private final BookMapper bookMapper;
    private final BookService bookService;
    private final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

    @Autowired
    public KafkaConsumer(BookMapper bookMapper, BookService bookService) {
        this.bookMapper = bookMapper;
        this.bookService = bookService;
    }

    @KafkaListener(topics = "reviews", groupId = "review-I")
    public void consumeReviewDTO(ConsumerRecord<String, ReviewDTO> message) {
        // message.value();
        throw new RuntimeException();
    }

    @RetryableTopic(attempts = "4"/*, backoff = @Backoff(delay = 1000, multiplier = 2)*/)
    @KafkaListener(topics = "book-dto", groupId = "book-I", containerFactory = "listener")
    public void consumeBookDTO(ConsumerRecord<String, BookDTO> message, Acknowledgment acknowledgment) {

        logger.info("The following message has been received: " + message.value());
        BookDTO bookDTO = message.value();
        Book book = bookMapper.toEntity(bookDTO);

        /**
         * TODO Check daca deja nu exista o carte de genul in baza de date. Daca exista, atunci nu mai
         * facem operatie de ADD pentru ca ar duce la duplicate.
         * Check ul putem sa l facem folosind acel BookIdentifierDTO!
         */
        if (bookDTO.getBookOperationType().equals(BookOperationType.ADD)) {
            /*bookService.addBook(book);*/
            logger.info("Book has been added into the database");
            throw new RuntimeException();
        }

        if (bookDTO.getBookOperationType().equals(BookOperationType.UPDATE)) {
            bookService.updateBook(book, bookDTO.getId());
            logger.info("Book has been updated inside the database");
        }

        if (bookDTO.getBookOperationType().equals(BookOperationType.DELETE)) {
            bookService.deleteBook(bookDTO.getId());
            logger.info("Book deletion process was activated");
        }
        acknowledgment.acknowledge(); // for committing the offsets
    }

    @DltHandler
    public void sendBookToDLT(ConsumerRecord<String, BookDTO> record, Acknowledgment acknowledgment) {
        logger.info("Message sent to DLT : " + record.value());
        acknowledgment.acknowledge();
    }
}
