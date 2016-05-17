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
     * Benchmark                     Mode  Cnt    Score   Error  Units
     CsvParserBenchmark.parse      avgt   20  173.738 ± 2.790  ns/op
     CsvParserBenchmark.parseTrim  avgt   20  241.522 ± 5.823  ns/op


     Benchmark                     Mode  Cnt    Score   Error  Units
     CsvParserBenchmark.parse      avgt   20  161.341 ± 0.519  ns/op
     CsvParserBenchmark.parseTrim  avgt   20  235.243 ± 1.048  ns/op


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
