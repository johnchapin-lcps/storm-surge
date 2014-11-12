storm-surge
===========

Storm Surge is an api that makes it easier to massively parallelize stream processing using [NVidia GPGPUs](http://www.nvidia.com/object/what-is-gpu-computing.html) with [Apache Storm](https://storm.incubator.apache.org/).

It’s about 200 lines of code that don’t actually do anything on their own. You need provide your own Cuda kernel (written in c) and Storm Bolt to do any processing. This code can be run anywhere you’ve got Storm, an NVidia GPGPU, JCuda and the Cuda SDK installed. 

Storm Surge provides a special kind of StormBolt which:
* Buffers input to the bolt in order to make it easier to consume by your cuda kernel
* Provides some helper functions which make it easier to move data between Storm & Cuda
* Follows the standard Storm protocol for moving data out of your bolt and on to the rest of your topology

It also provides a set of utility classes and functions which:
* Call the cuda compiler so you don’t need to pre-compile your kernels for any environment they might run on
* Make it easier to move data between java and cuda kernels.

## Getting Started

1. Install the [NVidia Cuda Toolkit](https://developer.nvidia.com/cuda-downloads)
2. Place [JCuda](http://www.jcuda.org/) binaries in your classpath
3. Extend storm.surge.CudaBolt 

## License

TBD
