/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package streams;

import java.util.Locale;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;

public class WordCountProcessorSupplier implements ProcessorSupplier<String, Processor> {
    @Override
    public Processor get() {
        return new WordCountProcessor();
    }

    private class WordCountProcessor implements Processor<String, String>{

        private ProcessorContext context;

        private KeyValueStore<String, Integer> kvStore;

        @Override
        public void init(ProcessorContext context) {
            this.context = context;
            this.context.schedule(1000);
            this.kvStore = (KeyValueStore<String, Integer>) context.getStateStore("Counts");
        }

        @Override
        public void process(String key, String value) {
        	System.out.println(key + "~~~~~~~~~~~~~" + value);
            String[] words = value.toLowerCase(Locale.getDefault()).split(" ");
            for (String word : words) {
                Integer oldValue = this.kvStore.get(word);
                if (oldValue == null) {
                    this.kvStore.put(word, 1);
                } else {
                    this.kvStore.put(word, oldValue + 1);
                }
            }
        }

        /**
         * Perform any periodic operations
         * @param timestamp
         */
        @Override
        public void punctuate(long timestamp) {
            try (KeyValueIterator<String, Integer> itr = this.kvStore.all()) {
                while (itr.hasNext()) {
                    KeyValue<String, Integer> entry = itr.next();
                    System.out.println("[" + entry.key + ", " + entry.value + "]");
                    context.forward(entry.key, entry.value.toString());
                }
                context.commit();
            }
        }

        @Override
        public void close() {
            this.kvStore.close();
        }
    }
}
