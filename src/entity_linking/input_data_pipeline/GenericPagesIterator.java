package entity_linking.input_data_pipeline;

import java.util.Iterator;

// Generic parser that returns an iterable of GenericSinglePages.
public interface GenericPagesIterator extends Iterator<GenericSinglePage> {
    public GenericPagesIterator hardCopy();
}
