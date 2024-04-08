package weka.datagenerators.classifiers.classification;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import com.google.gson.*;
import org.aeonbits.owner.ConfigCache;
import weka.core.*;
import weka.core.converters.ArffSaver;
import weka.datagenerators.ClassificationGenerator;
import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;
import wf.bitcoin.javabitcoindrpcclient.GenericRpcException;
import wf.bitcoin.javabitcoindrpcclient.config.RpcClientConfig;
import wf.bitcoin.javabitcoindrpcclient.config.RpcClientConfigI;
import wf.bitcoin.javabitcoindrpcclient.util.Chain;
import wf.bitcoin.javabitcoindrpcclient.util.Util;

public class BtcTransacGen extends ClassificationGenerator {
    static final Logger LOGGER = Logger.getLogger(BtcTransacGen.class.getName());

    /**
     * for serialization
     */
    //static final long serialVersionUID = 6069033710635728720L;

    /**
     * Number of attributes the dataset should have
     */
    protected int m_NumAttributes;

    /**
     * the number of transactions to generate
     */
    protected int m_NumTransactions;


    /**
     * Initializes the generator with default values
     */
    public BtcTransacGen() {
        super();

        setNumAttributes(defaultNumAttributes());
        setNumTransactions(defaultNumTransactions());
    }

    /**
     * Returns a string describing this data generator.
     *
     * @return a description of the data generator suitable for displaying in the
     * explorer/experimenter gui
     */
    public String globalInfo() {
        return "BtcTransacGen generates random Bitcoin transactions with transaction fees and sizes.";
    }


    /**
     * Generates a comment string that documentates the data generator. By default
     * this string is added at the beginning of the produced output as ARFF file
     * type, next after the options.
     *
     * @return string contains info about the generated rules
     */
    @Override
    public String generateStart() {
        StringBuilder result = new StringBuilder();

        result.append("%\n");
        result.append("% BitcoinTransactionGenerator - Random Bitcoin Transaction Generator\n");
        result.append("% Generates random Bitcoin transactions with transaction fees and sizes.\n");
        result.append("% Options:\n");
        result.append("%   -b <num_transactions>: Number of transactions to generate\n");
        result.append("%   -o <output_file.arff>: Output ARFF file name\n");
        result.append("%\n");

        return result.toString();
    }

    /**
     * Generates a comment string that documentats the data generator. By default
     * this string is added at the end of theproduces output as ARFF file type.
     *
     * @return string contains info about the generated rules
     * @throws Exception if the generating of the documentaion fails
     */
    @Override
    public String generateFinished() {
        // Additional information to include at the end of the ARFF file
        return "% End of Bitcoin transactions data.";
    }

    /**
     * Return if single mode is set for the given data generator mode depends on
     * option setting and or generator type.
     *
     * @return single mode flag
     * @throws Exception if mode is not set yet
     */
    @Override
    public boolean getSingleModeFlag() throws Exception {
        return false;
    }

    /**
     * Returns the revision string.
     *
     * @return the revision
     */
    @Override
    public String getRevision() {
        return RevisionUtils.extract("$Revision$");
    }

    /**
     * Returns an enumeration describing the available options.
     *
     * @return an enumeration of all the available options
     */
    @Override
    public Enumeration<Option> listOptions() {
        Vector<Option> result = enumToVector(super.listOptions());

        result.add(new Option("\tThe number of transactions to generate (default " + defaultNumTransactions() + ").",
                "n", 1, "-nt <numTransactions>"));

        return result.elements();
    }

    /**
     * Parses a list of options for this object.
     *
     * @param options the list of options as an array of strings
     * @throws Exception if an option is not supported
     */
    @Override
    public void setOptions(String[] options) throws Exception {
        String tmpStr;

        super.setOptions(options);

        tmpStr = Utils.getOption('b', options);
        if (tmpStr.length() != 0) {
            setNumTransactions(Integer.parseInt(tmpStr));
        } else {
            setNumTransactions(defaultNumTransactions());
        }
    }

    /**
     * Gets the current settings of the datagenerator.
     *
     * @return an array of strings suitable for passing to setOptions
     */
    @Override
    public String[] getOptions() {
        Vector<String> result;
        String[] options;
        int i;

        result = new Vector<String>();
        options = super.getOptions();
        for (i = 0; i < options.length; i++) {
            result.add(options[i]);
        }

        result.add("-b");
        result.add("" + getNumTransactions());

        return result.toArray(new String[result.size()]);
    }

    /**
     * returns the default number of attributes
     *
     * @return the default number of attributes
     */
    protected int defaultNumAttributes() {
        return 25;
    }

    /**
     * Sets the number of attributes the dataset should have.
     *
     * @param numAttributes the new number of attributes
     */
    public void setNumAttributes(int numAttributes) {
        m_NumAttributes = numAttributes;
    }

    /**
     * Gets the number of attributes that should be produced.
     *
     * @return the number of attributes that should be produced
     */
    public int getNumAttributes() {
        return m_NumAttributes;
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for displaying in the
     * explorer/experimenter gui
     */
    public String numAttributesTipText() {
        return "The number of attributes the generated data will contain.";
    }

    /**
     * returns the default number of transactions
     *
     * @return the default number of transactions
     */
    protected int defaultNumTransactions() {
        return 5; // Default number of transactions
    }

    /**
     * Sets the number of transactions to generate.
     *
     * @param numTransactions the new number of transactions
     */
    public void setNumTransactions(int numTransactions) {
        m_NumTransactions = numTransactions;
    }


    /**
     *
     * Parsing options
     */
    public void optionsParser(String[] options) throws Exception {

        String tmpStr = Utils.getOption('n', options);
        if (tmpStr.length() != 0) {
            setNumTransactions(Integer.parseInt(tmpStr));
        } else {
            setNumTransactions(defaultNumTransactions());
        }
    }

    /**
     * Gets the number of transactions to generate.
     *
     * @return the number of transactions to generate
     */
    public int getNumTransactions() {
        return m_NumTransactions;
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for displaying in the
     * explorer/experimenter gui
     */
    public String numTransactionsTipText() {
        return "The number of transactions to generate.";
    }

    /**
     * Initializes the format for the dataset produced.
     *
     * @return the format for the dataset
     * @throws Exception if the generating of the format failed
     */
    @Override
    public Instances defineDataFormat() throws Exception {
        ArrayList<Attribute> atts = new ArrayList<Attribute>();
        ArrayList<String> trustedValues = new ArrayList<String>(2);
        trustedValues.add("true");
        trustedValues.add("false");

        Attribute amount = new Attribute("amount");
        Attribute fee = new Attribute("fee");
        Attribute confirmations = new Attribute("confirmations");
        //Attribute trusted = new Attribute("trusted");
        Attribute txid = new Attribute("txid", (ArrayList<String>) null);
        Attribute wtxid = new Attribute("wtxid", (ArrayList<String>) null);
        Attribute time = new Attribute("time");
        Attribute timereceived = new Attribute("timereceived");
        Attribute bip125Replaceable = new Attribute("bip125_replaceable", (ArrayList<String>) null);
        Attribute details = new Attribute("details");
        Attribute hex = new Attribute("hex", (ArrayList<String>) null);
        Attribute receiving_address = new Attribute("receiving_address", (ArrayList<String>) null);

// Attributes for details array elements
        Attribute address_sender = new Attribute("address_sender", (ArrayList<String>) null);
        Attribute category = new Attribute("category", (ArrayList<String>) null);
        Attribute amount_sent = new Attribute("amount_sent");
        Attribute vout = new Attribute("vout");
        Attribute fee1 = new Attribute("fee1");
        Attribute abandoned = new Attribute("abandoned", trustedValues);

        Attribute hexValue = new Attribute("hex_value");


        // Add all attributes to the atts list
        atts.add(amount);
        atts.add(fee);
        atts.add(confirmations);
        atts.add(new Attribute("trusted", trustedValues));;
        atts.add(txid);
        atts.add(wtxid);
        atts.add(time);
        atts.add(timereceived);
        atts.add(bip125Replaceable);
        atts.add(details);
        atts.add(hex);
        atts.add(receiving_address);

// Add attributes for details array elements
        atts.add(address_sender);
        atts.add(category);
        atts.add(amount_sent);
        atts.add(vout);
        atts.add(fee1);
        atts.add(abandoned);

        atts.add(hexValue);



        // Create Instances object with the defined attributes
        Instances datasetFormat = new Instances(getRelationNameToUse(), atts, 0);

        // Set class index to the last attribute
        datasetFormat.setClassIndex(datasetFormat.numAttributes() - 1);

        return datasetFormat;
    }

    /**
     * Generates one example of the dataset.
     *
     * @return the generated example
     * @throws Exception if the format of the dataset is not yet defined
     */
    @Override
    public Instance generateExample() throws Exception {
        throw new UnsupportedOperationException("Single instance generation is not supported.");
    }

    /**
     * Generates all examples of the dataset.
     *
     * @return the generated dataset
     * @throws Exception if the format of the dataset is not yet defined
     */
    @Override
    public Instances generateExamples() throws Exception {
        System.out.println("alllooo");
        Instances dataset = defineDataFormat();
        dataset.setClassIndex(dataset.numAttributes() - 1); // Set class index

        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-7s] %5$s %n");
        URL url = new URL("http://user:WLMClI3cZ3ghE3diSTK-ENHSenP0bnthnbYmrAg7hcM@localhost:9997");

        BitcoinJSONRPCClient client = new BitcoinJSONRPCClient(url);
        Util.ensureRunningOnChain(Chain.REGTEST, client);

        Application.LOGGER.info("running");

        System.out.println(client.query("addnode", "bitcoin-node2:2223", "add"));
        //client.addNode("127.0.0.1:18445", "add");
        boolean isConnected = false;

// Loop until the node is connected or a timeout occurs
        long startTime = System.currentTimeMillis();
        long timeout = 30000; // 30 seconds timeout
        while (!isConnected && (System.currentTimeMillis() - startTime) < timeout) {
            // Fetch the added node info
            Object nodesInfo = client.query("getaddednodeinfo");
            System.out.println(nodesInfo);
            if (nodesInfo.toString().contains("true")) {
                isConnected = true;
                break;
            }
            // Check if connected, otherwise wait a bit before trying again
            if (!isConnected) {
                try {
                    Thread.sleep(1000); // Wait for 1 second
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Waiting for node connection was interrupted", e);
                }
            }
        }

        if (isConnected) {

            JsonRPCClient jsonRpcClient = new JsonRPCClient();
            String randomWalletName = Application.generateRandomString(10);


            try {
                Map<String, Object> result = jsonRpcClient.createWallet(randomWalletName);
                String walletName = (String) result.get("name");
                String warning = (String) result.get("warning");
                Application.LOGGER.info("Wallet created: " + walletName);
                if (warning != null) {
                    Application.LOGGER.warning("Warning: " + warning);
                }
            } catch (GenericRpcException e) {
                Application.LOGGER.severe("Error creating wallet: " + e.getMessage());
                //return; // Exit the function if an error occurs


                try {
                    Map<String, Object> result = jsonRpcClient.loadWallet(randomWalletName);
                    String walletName = (String) result.get("name");
                    String warning = (String) result.get("warning");
                    Application.LOGGER.info("Wallet loaded: " + walletName);
                    if (warning != null) {
                        Application.LOGGER.warning("Warning: " + warning);
                    }
                } catch (GenericRpcException eLoad) {
                    Application.LOGGER.severe("Error loading wallet: " + eLoad.getMessage());
                    //return; // Exit the function if an error occurs
                }


            }

            String addr1 = client.getNewAddress();
            List<String> generatedBlocksHashes = client.generateToAddress(510, addr1);

            System.out.println("YEEEESSSS" + jsonRpcClient.getBalance());


            for (int i = 0; i < getNumTransactions(); i++) {
                Instance instance = new DenseInstance(dataset.numAttributes()); // Generate instance using superclass method

                // Associate the instance with the dataset
                instance.setDataset(dataset);

                JsonObject transaction = Application.signRawTransactionWithKeyTest_P2SH_P2WPKH(client, addr1);

                System.out.println("gggggggggg " + transaction);

                // Set values from transactionObject to respective attributes
                instance.setValue(dataset.attribute("fee"), transaction.get("fee").getAsDouble());
                instance.setValue(dataset.attribute("confirmations"), transaction.get("confirmations").getAsInt());
                instance.setValue(dataset.attribute("trusted"), String.valueOf(transaction.get("trusted").getAsBoolean()));
                instance.setValue(dataset.attribute("txid"), transaction.get("txid").getAsString());
                instance.setValue(dataset.attribute("wtxid"), transaction.get("wtxid").getAsString());
                instance.setValue(dataset.attribute("time"), transaction.get("time").getAsLong());
                instance.setValue(dataset.attribute("timereceived"), transaction.get("timereceived").getAsLong());
                instance.setValue(dataset.attribute("bip125_replaceable"), transaction.get("bip125-replaceable").getAsString());
                instance.setValue(dataset.attribute("hex"), transaction.get("hex").getAsString());
                instance.setValue(dataset.attribute("receiving_address"), transaction.get("receiving_address").getAsString());


                JsonArray detailsArray = transaction.getAsJsonArray("details");

                    JsonObject detailObject = detailsArray.get(0).getAsJsonObject();

                    // Now you can use `i` as the index
                    // Set values for details array attributes
                    instance.setValue(dataset.attribute("fee1"), detailObject.get("fee").getAsDouble());
                    instance.setValue(dataset.attribute("abandoned"), String.valueOf(detailObject.get("abandoned").getAsBoolean()));
                    instance.setValue(dataset.attribute("address_sender"), detailObject.get("address").getAsString());
                    instance.setValue(dataset.attribute("category"), detailObject.get("category").getAsString());
                    instance.setValue(dataset.attribute("amount_sent"), detailObject.get("amount").getAsDouble());
                    instance.setValue(dataset.attribute("amount"), detailObject.get("amount").getAsDouble());

                instance.setValue(dataset.attribute("vout"), detailObject.get("vout").getAsInt());



                // Generate random transaction fee, size, sender address, and receiver address

                dataset.add(instance);
            }

            try {
                Map<String, Object> result = jsonRpcClient.unloadWallet(randomWalletName);
                String warning = (String) result.get("warning");
                Application.LOGGER.info("Wallet unloaded: " + randomWalletName);
                if (warning != null) {
                    Application.LOGGER.warning("Warning: " + warning);
                }
            } catch (GenericRpcException e) {
                Application.LOGGER.severe("Error unloading wallet: " + e.getMessage());
                //return; // Exit the function if an error occurs
            }
            System.out.println(client.query("addnode", "bitcoin-node2:2223", "remove"));

            return dataset;
        } else {
            System.out.println("Error connecting to second node, empty dataset.");
            return dataset;
        }
    }
    public static void main(String[] args) {

        LOGGER.info("here");
        System.out.println("HEEEEERRRREEe");
        //weka.core.logging.Logger.log(weka.core.logging.Logger.Level.INFO,"ooooooooooooooooooo");
        //TransactionsGenPlugin generator = new TransactionsGenPlugin();

        runDataGenerator(new BtcTransacGen(), args);
        /*try {
            generator.optionsParser(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }


        // Generate dataset
        try {
            Instances dataset = generator.generateFromApplication();
            // Output dataset to ARFF file

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }*/
    }

}
