package org.sfm.csv;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.sfm.csv.parser.CellConsumer;

import java.io.IOException;
import java.util.List;

@State(Scope.Benchmark)
public class CsvParserBenchmark {

    /**
     Benchmark                      Mode  Cnt    Score   Error  Units
     CsvParserBenchmark.parse       avgt   20  479.269 ± 1.984  ns/op
     CsvParserBenchmark.parseQuote  avgt   20  951.399 ± 5.064  ns/op
     CsvParserBenchmark.parseTrim   avgt   20  756.353 ± 2.033  ns/op
     Benchmark                      Mode  Cnt    Score   Error  Units
     CsvParserBenchmark.parse       avgt   20  469.553 ± 1.793  ns/op
     CsvParserBenchmark.parseQuote  avgt   20  996.914 ± 3.604  ns/op
     CsvParserBenchmark.parseTrim   avgt   20  736.330 ± 2.716  ns/op

     Benchmark                      Mode  Cnt    Score   Error  Units
     CsvParserBenchmark.parse       avgt   20  453.102 ± 1.281  ns/op
     CsvParserBenchmark.parseQuote  avgt   20  856.279 ± 3.323  ns/op
     CsvParserBenchmark.parseTrim   avgt   20  775.451 ± 2.041  ns/op


     Benchmark                      Mode  Cnt    Score   Error  Units
     CsvParserBenchmark.parse       avgt   20  428.510 ± 1.684  ns/op
     CsvParserBenchmark.parseQuote  avgt   20  866.024 ± 2.639  ns/op
     CsvParserBenchmark.parseTrim   avgt   20  778.768 ± 1.860  ns/op


     */
    public String csv = "val,val2  sdssddsds,lllll llll,sdkokokokokads<>Sddsdsds, adsdsadsad ,1, 3 ,4";
    public String csvQuote = "\"val\",\"val2  sdssddsds\",\"lllll llll\",\"sdkokokokokads<>Sddsdsds\",\"adsdsadsad\",\"1\",\"3\",\"4\"";


    public static final CsvParser.DSL dsl = CsvParser.dsl();

    public static final CsvParser.DSL tdsl = CsvParser.dsl().trimSpaces();

    @Benchmark
    public void parse(Blackhole blackhole) throws IOException {
        dsl.parse(csv, new MyCellConsumer(blackhole));
    }

    @Benchmark
    public void parseTrim(Blackhole blackhole) throws IOException {
        tdsl.parse(csv, new MyCellConsumer(blackhole));
    }

    @Benchmark
    public void parseQuote(Blackhole blackhole) throws IOException {
        dsl.parse(csvQuote, new MyCellConsumer(blackhole));
    }

    public static void main(String[] args) throws IOException {
        new CsvParserBenchmark().parseQuote(null);
    }


    private static class MyCellConsumer implements CellConsumer {
        private final Blackhole blackhole;

        public MyCellConsumer(Blackhole blackhole) {
            this.blackhole= blackhole;
        }

        @Override
        public void newCell(char[] chars, int offset, int length) {
            if (blackhole != null) {
                blackhole.consume(chars);
                blackhole.consume(offset);
                blackhole.consume(length);
            }
        }

        @Override
        public void endOfRow() {

        }

        @Override
        public void end() {

        }
    }
}
