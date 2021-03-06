/*
 * Copyright 2013 Proofpoint Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.kairosdb.core.aggregator;

import com.google.inject.Inject;
import org.kairosdb.core.DataPoint;
import org.kairosdb.core.aggregator.annotation.AggregatorName;
import org.kairosdb.core.datapoints.DoubleDataPoint;
import org.kairosdb.core.datapoints.DoubleDataPointFactory;
import org.kairosdb.core.datastore.DataPointGroup;
import org.kairosdb.core.datastore.Sampling;
import org.kairosdb.core.datastore.TimeUnit;

@AggregatorName(name = "sampler", description = "Computes the sampling rate of change for the data points.")
public class SamplerAggregator implements Aggregator
{
	private long m_sampling;
	private DoubleDataPointFactory m_dataPointFactory;

	@Inject
	public SamplerAggregator(DoubleDataPointFactory dataPointFactory)
	{
		m_dataPointFactory = dataPointFactory;
		m_sampling = 1L;
	}

	public DataPointGroup aggregate(DataPointGroup dataPointGroup)
	{
		return (new SamplerDataPointAggregator(dataPointGroup));
	}

	@Override
	public boolean canAggregate(String groupType)
	{
		return DataPoint.GROUP_NUMBER.equals(groupType);
	}

	public void setUnit(TimeUnit timeUnit)
	{
		m_sampling = new Sampling(1, timeUnit).getSampling();
	}


	private class SamplerDataPointAggregator extends AggregatedDataPointGroupWrapper
	{
		public SamplerDataPointAggregator(DataPointGroup innerDataPointGroup)
		{
			super(innerDataPointGroup);
		}

		@Override
		public DataPoint next()
		{
			final double x0 = currentDataPoint.getDoubleValue();
			final long y0 = currentDataPoint.getTimestamp();

			//This defaults the rate to 0 if no more data points exists
			double x1 = 0;
			long y1 = y0 + 1;

			if (hasNextInternal())
			{
				currentDataPoint = nextInternal();

				x1 = currentDataPoint.getDoubleValue();
				y1 = currentDataPoint.getTimestamp();

				if (y1 == y0)
				{
					throw new IllegalStateException(
							"The sampler aggregator cannot compute rate for data points with the same time stamp.  " +
									"You must precede sampler with another aggregator.");
				}
			}
			double rate = x1 / (y1 - y0) * m_sampling;

			return (m_dataPointFactory.createDataPoint(y1, rate));
		}
	}
}
