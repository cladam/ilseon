package com.ilseon.util

// Here are commonly used stop words for English and Swedish, based on typical NLP/word-cloud libraries.
// Add new words or sets by adding to the allStopWords variable at the bottom
val stopWords_EN = setOf(
    "a", "an", "the",
    "this", "that", "these", "those",
    "some", "any", "each", "every",
    "i", "you", "he", "she", "it", "we", "they",
    "me", "him", "her", "us", "them",
    "my", "your", "his", "her", "its", "our", "their",
    "in", "on", "at", "by", "to", "from",
    "for", "of", "with", "about", "over", "under",
    "between", "through", "during",
    "and", "or", "but", "so", "if", "because", "while", "although",
    "as",
    "is", "am", "are", "was", "were",
    "be", "been", "being",
    "do", "does", "did",
    "have", "has", "had",
    "will", "would", "shall", "should",
    "can", "could", "may", "might", "must",
    "not", "very", "just", "only", "also",
    "even", "then", "there", "here"
)

val stopWords_SE = setOf(
    "en", "ett", "den", "det", "de",
    "denna", "detta", "dessa",
    "ingen", "inget", "inga",
    "någon", "något", "några",
    "jag", "du", "han", "hon", "vi", "ni", "de",
    "mig", "dig", "honom", "henne", "oss", "er",
    "min", "mitt", "mina", "din", "ditt", "dina",
    "sin", "sitt", "sina",
    "vår", "vårt", "våra",
    "i", "på", "till", "för", "från",
    "med", "utan", "över", "under",
    "mellan", "genom", "efter", "före",
    "och", "eller", "men", "så", "att",
    "som", "när", "medan", "eftersom", "fast",
    "om", "än", "då",
    "är", "var", "vara", "varit",
    "ha", "hade", "har",
    "ska", "skulle",
    "kan", "kunde",
    "vill", "ville",
    "måste", "bör",
    "inte", "också", "bara", "redan", "ännu",
    "här", "där", "då", "sedan",
    "mycket", "många", "några", "sådan"
)

val allStopWords = stopWords_EN + stopWords_SE
