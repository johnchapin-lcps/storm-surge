package storm.surge;

import static jcuda.driver.JCudaDriver.cuCtxCreate;
import static jcuda.driver.JCudaDriver.cuDeviceGet;
import static jcuda.driver.JCudaDriver.cuInit;
import static jcuda.driver.JCudaDriver.cuModuleGetFunction;
import static jcuda.driver.JCudaDriver.cuModuleLoad;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import jcuda.driver.CUcontext;
import jcuda.driver.CUdevice;
import jcuda.driver.CUfunction;
import jcuda.driver.CUmodule;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.tuple.Tuple;

public abstract class CudaBolt extends BufferedBolt {

	private String cuFilePath;
	private String ptxFilePath;
	private String functionName;
	private CUfunction function;

	protected CUfunction getFunction() {
		return function;
	}

	public CudaBolt(String aCuFilePath, int bufferSize, String aFunctionName)
			throws IOException {
		super(bufferSize);
		cuFilePath = aCuFilePath;
		functionName = aFunctionName;
	}

	public void prepare(Map stormConf, TopologyContext context,
			OutputCollector collector) {

		try {
			String ptxFile = preparePtxFile(cuFilePath);
			function = getFunctionByName(ptxFile, functionName);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected int[][] tuplesToArray(List<Tuple> tuples, int numDimensions) {
		int numItems = tuples.size();
		int[][] featureDistances = new int[numItems][numDimensions];
		for (int i = 0; i < numItems; i++) {
			for (int j = 0; j < numDimensions; j++) {
				int[] value = (int[]) tuples.get(i).getValue(0);
				featureDistances[i][j] = value[j];
			}
		}

		return featureDistances;
	}

	/**
	 * The extension of the given file name is replaced with "ptx". If the file
	 * with the resulting name does not exist, it is compiled from the given
	 * file using NVCC. The name of the PTX file is returned.
	 *
	 * @param cuFileName
	 *            The name of the .CU file
	 * @return The name of the PTX file
	 * @throws IOException
	 *             If an I/O error occurs
	 */

	private String preparePtxFile(String cuFileName)
			throws FileNotFoundException, IOException {
		int endIndex = cuFileName.lastIndexOf('.');
		if (endIndex == -1) {
			endIndex = cuFileName.length() - 1;
		}
		String ptxFileName = cuFileName.substring(0, endIndex + 1) + "ptx";
		File ptxFile = new File(ptxFileName);

		/*
		 * if (ptxFile.exists()) { return ptxFileName; }
		 */

		File cuFile = new File(cuFileName);
		if (!cuFile.exists()) {
			throw new FileNotFoundException("Input file not found: "
					+ cuFileName);
		}
		String modelString = "-m" + System.getProperty("sun.arch.data.model");
		String command = "nvcc " + modelString + " -arch=sm_20 " + " -ptx "
				+ cuFile.getPath() + " -o " + ptxFileName;

		System.out.println("Executing\n" + command);
		Process process = Runtime.getRuntime().exec(command);

		String errorMessage = new String(toByteArray(process.getErrorStream()));
		String outputMessage = new String(toByteArray(process.getInputStream()));
		int exitValue = 0;
		try {
			exitValue = process.waitFor();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while waiting for nvcc output",
					e);
		}

		if (exitValue != 0) {
			System.out.println("nvcc process exitValue " + exitValue);
			System.out.println("errorMessage:\n" + errorMessage);
			System.out.println("outputMessage:\n" + outputMessage);
			throw new IOException("Could not create .ptx file: " + errorMessage);
		}

		System.out.println("Finished creating PTX file");
		return ptxFileName;
	}

	/**
	 * Fully reads the given InputStream and returns it as a byte array
	 *
	 * @param inputStream
	 *            The input stream to read
	 * @return The byte array containing the data from the input stream
	 * @throws IOException
	 *             If an I/O error occurs
	 */
	private static byte[] toByteArray(InputStream inputStream)
			throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte buffer[] = new byte[8192];
		while (true) {
			int read = inputStream.read(buffer);
			if (read == -1) {
				break;
			}
			baos.write(buffer, 0, read);
		}
		return baos.toByteArray();
	}

	private static CUfunction getFunctionByName(String ptxFileName,
			String aFunctionName) {
		// Initialize the driver and create a context for the first device.
		cuInit(0);
		CUdevice device = new CUdevice();
		cuDeviceGet(device, 0);
		CUcontext context = new CUcontext();
		cuCtxCreate(context, 0, device);

		// Load the ptx file.
		CUmodule module = new CUmodule();
		cuModuleLoad(module, ptxFileName);

		// Obtain a function pointer to the function.
		CUfunction function = new CUfunction();
		cuModuleGetFunction(function, module, aFunctionName);

		return function;
	}
}
