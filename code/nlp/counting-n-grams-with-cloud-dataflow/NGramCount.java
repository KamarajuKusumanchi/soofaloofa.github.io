/*
 * NGramCount
 */

package com.sookocheff.cloud.dataflow.examples;

import com.google.cloud.dataflow.sdk.Pipeline;
import com.google.cloud.dataflow.sdk.io.TextIO;
import com.google.cloud.dataflow.sdk.options.DataflowPipelineOptions;
import com.google.cloud.dataflow.sdk.options.Default;
import com.google.cloud.dataflow.sdk.options.DefaultValueFactory;
import com.google.cloud.dataflow.sdk.options.Description;
import com.google.cloud.dataflow.sdk.options.PipelineOptions;
import com.google.cloud.dataflow.sdk.options.PipelineOptionsFactory;
import com.google.cloud.dataflow.sdk.transforms.Aggregator;
import com.google.cloud.dataflow.sdk.transforms.Count;
import com.google.cloud.dataflow.sdk.transforms.DoFn;
import com.google.cloud.dataflow.sdk.transforms.PTransform;
import com.google.cloud.dataflow.sdk.transforms.ParDo;
import com.google.cloud.dataflow.sdk.transforms.Sum;
import com.google.cloud.dataflow.sdk.transforms.Top;
import com.google.cloud.dataflow.sdk.transforms.SerializableComparator;
import com.google.cloud.dataflow.sdk.util.gcsfs.GcsPath;
import com.google.cloud.dataflow.sdk.values.KV;
import com.google.cloud.dataflow.sdk.values.PCollection;

import java.io.IOException;
import java.util.*;


/**
 * Count ngrams.
 *
 * <p> To execute this pipeline locally, specify general pipeline configuration:
 * <pre>{@code
 *   --project=YOUR_PROJECT_ID
 * }
 *
 * <p> Specify the n-value with --n, defaults to 2 (bigrams)
 * <pre>{@code
 *   --n=2
 * }
 * </pre>
 * </pre>
 * and a local output file or output prefix on GCS:
 * <pre>{@code
 *   --output=[YOUR_LOCAL_FILE | gs://YOUR_OUTPUT_PREFIX]
 * }</pre>
 *
 * <p> To execute this pipeline using the Dataflow service, specify pipeline configuration:
 * <pre>{@code
 *   --project=YOUR_PROJECT_ID
 *   --stagingLocation=gs://YOUR_STAGING_DIRECTORY
 *   --runner=BlockingDataflowPipelineRunner
 * }
 * </pre>
 * and an output prefix on GCS:
 * <pre>{@code
 *   --output=gs://YOUR_OUTPUT_PREFIX
 * }</pre>
 *
 * <p> The input file defaults to {@code gs://dataflow-samples/shakespeare/kinglear.txt} and can be
 * overridden with {@code --inputFile}.
 */
public class NGramCount {

  /**
   * This DoFn tokenizes lines of text into individual ngrams; we pass it to a ParDo in the
   * pipeline.
   */
  static class ExtractNGramsFn extends DoFn<String, String> {
    private static final long serialVersionUID = 0;

    private Integer n;

    public ExtractNGramsFn(Integer n) {
      this.n = n;
    }

    private final Aggregator<Long, Long> ngramCount =
        createAggregator("ngramCount", new Sum.SumLongFn());

    @Override
    public void processElement(ProcessContext c) {
      // Split the line into words (splits at any whitespace character, grouping
      // whitespace together).
      String[] words = c.element().split("\\s+");

      // Group into ngrams
      List<String> ngrams = new ArrayList<String>();
      for (int i = 0; i <= words.length-this.n; i++) {
        StringBuilder ngram = new StringBuilder();
        for (int j = 0; j < this.n; j++) {
          if (j > 0) {
            ngram.append("\t");
          }
          ngram.append(words[i+j]);
        }
        ngrams.add(ngram.toString());
      }

      // Output each ngram encountered into the output PCollection.
      for (String ngram : ngrams) {
        if (!ngram.isEmpty()) {
          ngramCount.addValue(1L);
          c.output(ngram);
        }
      }
    }
  }

  /** A DoFn that converts an NGram and Count into a printable string. */
  public static class FormatAsTextFn extends DoFn<List<KV<String, Long>>, String> {
    private static final long serialVersionUID = 0;

    @Override
    public void processElement(ProcessContext c) {

      for (KV<String, Long> item : c.element()) {
        String ngram = item.getKey();
        long count = item.getValue();
        c.output(ngram + "\t" + count);
      }
    }
  }

  /**
   * A PTransform that converts a PCollection containing lines of text into a PCollection of
   * formatted word counts.
   *
   * <p> Concept #4: This is a custom composite transform that bundles two transforms (ParDo and
   * Count) as a reusable PTransform subclass. Using composite transforms allows for easy reuse,
   * modular testing, and an improved monitoring experience.
   */
  public static class CountNGrams
    extends PTransform<PCollection<String>, PCollection<List<KV<String, Long>>>> {

    private static final long serialVersionUID = 0;

    private Integer n;
    private Integer top;

    public CountNGrams(Integer n) {
      this.n = n;
      this.top = new Integer(100);
    }

    public CountNGrams(Integer n, Integer top) {
      this.n = n;
      this.top = top;
    }

    @Override
    public PCollection<List<KV<String, Long>>> apply(PCollection<String> lines) {

      // Convert lines of text into individual ngrams.
      PCollection<String> ngrams = lines.apply(
          ParDo.of(new ExtractNGramsFn(this.n)));

      // Count the number of times each ngram occurs.
      PCollection<KV<String, Long>> ngramCounts =
          ngrams.apply(Count.<String>perElement());

      // Find the top ngrams in the corpus
      PCollection<List<KV<String, Long>>> topNgrams = 
          ngramCounts.apply(Top.of(this.top, new SerializableComparator<KV<String, Long>>() {
                    private static final long serialVersionUID = 0;

                    @Override
                    public int compare(KV<String, Long> o1, KV<String, Long> o2) {
                      return Long.compare(o1.getValue(), o2.getValue());
                    }
                  }).withoutDefaults());
      
      return topNgrams;
    }
  }

  /**
   * Options supported by {@link NGramCount}.
   *
   * <p> Concept #5: defining your own configuration options. Here, you can add your own args to be
   * processed by the command-line parser, and specify default values for them. You can then access
   * the options values in your pipeline code.
   *
   * <p> Inherits standard configuration options.
   */
  public static interface NGramCountOptions extends PipelineOptions {
    @Description("Number of n-grams to model.")
    @Default.Integer(2)
    Integer getN();
    void setN(Integer value);

    @Description("Number top ngram counts to return.")
    @Default.Integer(100)
    Integer getTop();
    void setTop(Integer value);

    @Description("Path of the file to read from")
    @Default.String("gs://dataflow-samples/shakespeare/kinglear.txt")
    String getInputFile();
    void setInputFile(String value);

    @Description("Path of the file to write to")
    @Default.InstanceFactory(OutputFactory.class)
    String getOutput();
    void setOutput(String value);

    /**
     * Returns gs://${STAGING_LOCATION}/"counts.txt" as the default destination.
     */
    public static class OutputFactory implements DefaultValueFactory<String> {
      @Override
      public String create(PipelineOptions options) {
        DataflowPipelineOptions dataflowOptions = options.as(DataflowPipelineOptions.class);
        if (dataflowOptions.getStagingLocation() != null) {
          return GcsPath.fromUri(dataflowOptions.getStagingLocation())
              .resolve("counts.txt").toString();
        } else {
          throw new IllegalArgumentException("Must specify --output or --stagingLocation");
        }
      }
    }

  }

  public static void main(String[] args) throws IOException {
    NGramCountOptions options = PipelineOptionsFactory.fromArgs(args).withValidation()
      .as(NGramCountOptions.class);
    Pipeline p = Pipeline.create(options);

    p.apply(TextIO.Read.named("ReadLines").from(options.getInputFile()))
     .apply(new CountNGrams(options.getN(), options.getTop()))
     .apply(ParDo.of(new FormatAsTextFn()))
     .apply(TextIO.Write.named("WriteCounts").to(options.getOutput()));

    p.run();
  }
}
