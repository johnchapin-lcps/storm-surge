package storm.surge;

import java.util.ArrayList;

import backtype.storm.task.OutputCollector;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;

public abstract class BufferedBolt extends BaseRichBolt{
	private static final int DEFAULT_BUFFER_SIZE = 10000;
	protected int _bufferSize;
	protected ArrayList<Tuple> _tuples;
	protected OutputCollector _collector;
	
	public BufferedBolt() {
		this(DEFAULT_BUFFER_SIZE);
	}
	
	public BufferedBolt(int bufferSize) {
		_bufferSize = bufferSize;
	}

	public void execute(Tuple tuple) {
		_tuples.add(tuple);
		
		if(_tuples.size() >= _bufferSize) {
			processTuples();
			
			_tuples.clear();
		}
		
		_collector.ack(tuple);
	}
	
	protected abstract void processTuples();
	
	@Override
	public void cleanup() {
		processTuples();
		super.cleanup();
	}
	
}
