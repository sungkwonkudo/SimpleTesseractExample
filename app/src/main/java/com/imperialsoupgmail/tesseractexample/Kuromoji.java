package com.imperialsoupgmail.tesseractexample;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by wyki on 11/20/17.
 */

public class Kuromoji implements Callable {

    final String words;
    Tokenizer tokenizer;
    Kuromoji(String input, Tokenizer tokenizer) {
        this.words = input;
        this.tokenizer = tokenizer;
    }

    @Override
    public List<Token> call() throws Exception {
        List<Token> tokenList = new ArrayList<>();
        for(Token token : tokenizer.tokenize(words)){
            tokenList.add(token);
        }
        return tokenList;
    }
}
