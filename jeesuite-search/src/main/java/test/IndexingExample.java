package test;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class IndexingExample {
  private static final String INDEX_DIR = "/Users/jiangwei/datas/lucene";

  private IndexWriter createWriter() throws IOException {
    FSDirectory dir = FSDirectory.open(Paths.get(INDEX_DIR));
    IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
    IndexWriter writer = new IndexWriter(dir, config);
    return writer;
  }

  private List<Document> createDocs() {
    List<Document> docs = new ArrayList<>();

    FieldType titleType = new FieldType();
    titleType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
    titleType.setStored(true);
    titleType.setTokenized(true);

    FieldType authorType = new FieldType();
    authorType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
    authorType.setStored(true);
    authorType.setOmitNorms(true);
    authorType.setTokenized(true);
    authorType.setStoreTermVectors(true);

    FieldType summaryType = new FieldType();
    summaryType.setIndexOptions(IndexOptions.DOCS);
    summaryType.setStored(true);
    summaryType.setTokenized(true);
    summaryType.setStoreTermVectors(true);
    summaryType.setStoreTermVectorPositions(true);
    summaryType.setStoreTermVectorOffsets(true);
    summaryType.setStoreTermVectorPayloads(true);

    FieldType ratingType = new FieldType();
    ratingType.setDimensions(1, Integer.BYTES);
    ratingType.setStored(true);

    Document doc1 = new Document();
    doc1.add(new StringField("asin", "B005XSS8VC", Field.Store.YES));
    doc1.add(new SortedSetDocValuesField("format", new BytesRef("kindle")));
    Field titleField1 = new Field("title", "What's New in Java 7", titleType);
    titleField1.setBoost(3.0f);
    doc1.add(titleField1);
    doc1.add(new SortedDocValuesField("publisher", new BytesRef("O'Reilly Media")));
    doc1.add(new Field("author", "Madhusudhan Konda", authorType));
    doc1.add(new Field("summary", "Java 7 has a number of features that will please developers. Madhusudhan Konda provides an overview of these, including strings in switch statements, multi-catch exception handling, try-with-resource statements, the new File System API, extensions of the JVM, support for dynamically-typed languages, and the fork and join framework for task parallelism.", summaryType));
    doc1.add(new NumericDocValuesField("page", 19));
    doc1.add(new LegacyIntField("size", 148, Field.Store.YES));
    doc1.add(new SortedNumericDocValuesField("price", 0));
    doc1.add(new IntPoint("rating", 1));
    doc1.add(new StringField("rating_display", "1", Field.Store.YES));
    docs.add(doc1);

    Document doc2 = new Document();
    doc2.add(new StringField("isbn", "978-0071809252", Field.Store.YES));
    doc2.add(new StringField("asin", "B00J4XLILY", Field.Store.YES));
    doc2.add(new SortedSetDocValuesField("format", new BytesRef("paperback")));
    doc2.add(new SortedSetDocValuesField("format", new BytesRef("kindle")));
    Field titleField2 = new Field("title", "Java: A Beginner's Guide, Sixth Edition", titleType);
    titleField2.setBoost(3.0f);
    doc2.add(titleField2);
    doc2.add(new SortedDocValuesField("publisher", new BytesRef("McGraw-Hill Education")));
    doc2.add(new Field("author", "Herbert Schildt", authorType));
    doc2.add(new Field("summary", "Fully updated for Java Platform, Standard Edition 8 (Java SE 8), Java: A Beginner's Guide, Sixth Edition gets you started programming in Java right away. Bestselling programming author Herb Schildt begins with the basics, such as how to create, compile, and run a Java program. He then moves on to the keywords, syntax, and constructs that form the core of the Java language. This Oracle Press resource also covers some of Java's more advanced features, including multithreaded programming, generics, and Swing. Of course, new Java SE 8 features such as lambda expressions and default interface methods are described. An introduction to JavaFX, Java's newest GUI, concludes this step-by-step tutorial.", summaryType));
    doc2.add(new NumericDocValuesField("page", 728));
    doc2.add(new LegacyIntField("size", 53941, Field.Store.YES));
    doc2.add(new SortedNumericDocValuesField("price", 4639));
    doc2.add(new SortedNumericDocValuesField("price", 3856));
    doc2.add(new SortedNumericDocValuesField("price", 3341));
    doc2.add(new IntPoint("rating", 5));
    doc2.add(new StringField("rating_display", "5", Field.Store.YES));
    docs.add(doc2);

    Document doc3 = new Document();
    doc3.add(new StringField("asin", "B00B8V09HY", Field.Store.YES));
    doc3.add(new StringField("isbn", "978-0321356680", Field.Store.YES));
    doc3.add(new SortedSetDocValuesField("format", new BytesRef("kindle")));
    doc3.add(new SortedSetDocValuesField("format", new BytesRef("paperback")));
    Field titleField3 = new Field("title", "Effective Java: A Programming Language Guide", titleType);
    titleField3.setBoost(3.0f);
    doc3.add(titleField3);
    doc3.add(new SortedDocValuesField("publisher", new BytesRef("Addison-Wesley Professional")));
    doc3.add(new Field("author", "Joshua Bloch", authorType));
    doc3.add(new Field("summary", "Are you looking for a deeper understanding of the Java™ programming language so that you can write code that is clearer, more correct, more robust, and more reusable? Look no further! Effective Java™, Second Edition, brings together seventy-eight indispensable programmer’s rules of thumb: working, best-practice solutions for the programming challenges you encounter every day. This highly anticipated new edition of the classic, Jolt Award-winning work has been thoroughly updated to cover Java SE 5 and Java SE 6 features introduced since the first edition. Bloch explores new design patterns and language idioms, showing you how to make the most of features ranging from generics to enums, annotations to autoboxing. Each chapter in the book consists of several “items” presented in the form of a short, standalone essay that provides specific advice, insight into Java platform subtleties, and outstanding code examples. The comprehensive descriptions and explanations for each item illuminate what to do, what not to do, and why.", summaryType));
    doc3.add(new NumericDocValuesField("page", 374));
    doc3.add(new LegacyIntField("size", 1957, Field.Store.YES));
    doc3.add(new SortedNumericDocValuesField("price", 3892));
    doc3.add(new SortedNumericDocValuesField("price", 6200));
    doc3.add(new IntPoint("rating", 4));
    doc3.add(new StringField("rating_display", "4", Field.Store.YES));
    docs.add(doc3);

    Document doc4 = new Document();
    doc4.add(new StringField("isbn", "978-1935182955", Field.Store.YES));
    doc4.add(new SortedSetDocValuesField("format", new BytesRef("paperback")));
    Field titleField4 = new Field("title", "Spring Batch in Action", titleType);
    titleField4.setBoost(3.0f);
    doc4.add(titleField4);
    doc4.add(new SortedDocValuesField("publisher", new BytesRef("Manning Pubns Co")));
    doc4.add(new Field("author", "Arnaud Cogoluegnes", authorType));
    doc4.add(new Field("author", "Thierry Templier", authorType));
    doc4.add(new Field("author", "Gary Gregory", authorType));
    doc4.add(new Field("author", "Olivier Bazoud", authorType));
    doc4.add(new Field("summary", "DESCRIPTION Even though running batch processes is an everyday task in almost all IT departments, Java developers have had few options for writing batch applications. The result? No standards, poor code reusability, numerous in-house solutions, and lots of frustrated developers. Jointly developed by SpringSource and Accenture, Spring Batch fills this critical gap by providing a robust and convenient framework for writing batch applications that process large volumes of information, automate repetitive tasks, and synchronize internal systems. Spring Batch in Action is a comprehensive, in-depth guide to writing batch applications using Spring Batch. Written for developers who have basic knowledge of Java and the Spring lightweight container, it provides both a best-practices approach to writing batch jobs and comprehensive coverage of the Spring Batch framework. KEY POINTS * Complete guide to the Spring Batch framework * Numerous real-world examples * Covers basics, best practices and advanced topics", summaryType));
    doc4.add(new NumericDocValuesField("page", 479));
    doc4.add(new SortedNumericDocValuesField("price", 6957));
    doc4.add(new SortedNumericDocValuesField("price", 5386));
    docs.add(doc4);

    Document doc5 = new Document();
    doc5.add(new StringField("isbn", "978-0321321367", Field.Store.YES));
    doc5.add(new StringField("isbn", "978-1292026152", Field.Store.YES));
    doc5.add(new SortedSetDocValuesField("format", new BytesRef("hardcover")));
    doc5.add(new SortedSetDocValuesField("format", new BytesRef("paperback")));
    Field titleField5 = new Field("title", "Introduction to Data Mining", titleType);
    titleField5.setBoost(3.0f);
    doc5.add(titleField5);
    doc5.add(new SortedDocValuesField("publisher", new BytesRef("Pearson Education Limited")));
    doc5.add(new Field("author", "Pang-Ning Tan", authorType));
    doc5.add(new Field("author", "Michael Steinbach", authorType));
    doc5.add(new Field("author", "Vipin Kumar", authorType));
    doc5.add(new Field("summary", "Introduction to Data Mining presents fundamental concepts and algorithms for those learning data mining for the first time. Each major topic is organized into two chapters, beginning with basic concepts that provide necessary background for understanding each data mining technique, followed by more advanced concepts and algorithms. ", summaryType));
    doc5.add(new LegacyIntField("page", 769, Field.Store.YES));
    doc5.add(new SortedNumericDocValuesField("price", 10107));
    doc5.add(new SortedNumericDocValuesField("price", 9358));
    docs.add(doc5);

    Document doc6 = new Document();
    doc6.add(new StringField("isbn", "978-1617290657", Field.Store.YES));
    doc6.add(new SortedSetDocValuesField("format", new BytesRef("paperback")));
    doc6.add(new Field("title", "Functional Programming in Scala", titleType));
    doc6.add(new SortedDocValuesField("publisher", new BytesRef("Manning Pubns Co")));
    doc6.add(new Field("author", "Paul Chiusano", authorType));
    doc6.add(new Field("author", " Runar Bjarnason", authorType));
    doc6.add(new Field("summary", "DESCRIPTION Functional programming (FP) is a programming style emphasizing functions that return consistent and predictable results regardless of a program's state. As a result, functional code is easier to test and reuse, simpler to parallelize, and less prone to bugs. Scala is an emerging JVM language that offers strong support for FP. Its familiar syntax and transparent interoperability with existing Java libraries make Scala a great place to start learning FP. Functional Programming in Scala is a serious tutorial for programmers looking to learn FP and apply it to the everyday business of coding. The book guides readers from basic techniques to advanced topics in a logical, concise, and clear progression. In it, they'll find concrete examples and exercises that open up the world of functional programming. RETAIL SELLING POINTS Covers the practical benefits of Functional Programming Offers concrete examples and exercises Logically progresses from basic techniques to advance topics AUDIENCE No prior experience with FP or Scala is required. Perfect for programmers familiar with FP in other languages wishing to apply their knowledge in Scala. ABOUT THE TECHNOLOGY Functional programming (FP) is a programming style emphasizing functions that return consistent and predictable results regardless of a program's state. Scala is an emerging JVM language that offers strong support for FP.", summaryType));
    doc6.add(new LegacyIntField("page", 300, Field.Store.YES));
    doc6.add(new SortedNumericDocValuesField("price", 4887));
    doc6.add(new SortedNumericDocValuesField("price", 4297));
    docs.add(doc6);

    return docs;
  }

  private IndexSearcher createSearcher() throws IOException {
    Directory dir = FSDirectory.open(Paths.get(INDEX_DIR));
    IndexReader reader = DirectoryReader.open(dir);
    IndexSearcher searcher = new IndexSearcher(reader);
    return searcher;
  }

  public static void main(String[] args) throws IOException, ParseException {
    // indexing
    IndexingExample app = new IndexingExample();
    IndexWriter writer = app.createWriter();
    writer.deleteAll();
    List<Document> docs = app.createDocs();
    writer.addDocuments(docs);
    writer.deleteDocuments(new Term("isbn", "978-0321321367"));
    writer.commit();
    writer.close();

    // search
    IndexSearcher searcher = app.createSearcher();
    QueryParser qp = new QueryParser("title", new StandardAnalyzer());

    Query q1 = qp.parse("java");
    TopDocs hits = searcher.search(q1, 10);
    System.out.println(hits.totalHits + " docs found for the query \"" + q1.toString() + "\"");
    int num = 0;
    for (ScoreDoc sd : hits.scoreDocs) {
      Document d = searcher.doc(sd.doc);
      System.out.println(String.format("#%d: %s (rating=%s)", ++num, d.get("title"), d.get("rating_display")));
    }

    System.out.println("");
    Query q2 = qp.parse("java AND program*");
    hits = searcher.search(q2, 10);
    System.out.println(hits.totalHits + " docs found for the query \"" + q2.toString() + "\"");
    num = 0;
    for (ScoreDoc sd : hits.scoreDocs) {
      Document d = searcher.doc(sd.doc);
      System.out.println(String.format("#%d: %s (rating=%s)", ++num, d.get("title"), d.get("rating_display")));
    }

    System.out.println("");
    Query q3 = qp.parse("java OR scala");
    hits = searcher.search(q3, 10);
    System.out.println(hits.totalHits + " docs found for the query \"" + q3.toString() + "\"");
    num = 0;
    for (ScoreDoc sd : hits.scoreDocs) {
      Document d = searcher.doc(sd.doc);
      System.out.println(String.format("#%d: %s (rating=%s)", ++num, d.get("title"), d.get("rating_display")));
    }

    System.out.println("");
    // PointValues で範囲検索する
    TermQuery tq = new TermQuery(new Term("title", "java"));
    Query prq = IntPoint.newRangeQuery("rating", 3, Integer.MAX_VALUE);
    BooleanQuery.Builder builder = new BooleanQuery.Builder();
    Query q4 = builder
        .add(tq, BooleanClause.Occur.MUST)
        .add(prq, BooleanClause.Occur.FILTER)  // Occur.FILTER を指定した節はスコア計算に影響しない
        .build();
    hits = searcher.search(q4, 10);
    System.out.println(hits.totalHits + " docs found for the query \"" + q4.toString() + "\"");
    num = 0;
    for (ScoreDoc sd : hits.scoreDocs) {
      Document d = searcher.doc(sd.doc);
      System.out.println(String.format("#%d: %s (rating=%s)", ++num, d.get("title"), d.get("rating_display")));
    }

  }
}
