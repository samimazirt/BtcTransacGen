package weka.datagenerators.classifiers.classification;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import com.google.gson.*;


import wf.bitcoin.javabitcoindrpcclient.*;

class JsonRPCClient extends BitcoinJSONRPCClient {

    public JsonRPCClient(URL url) throws MalformedURLException {
        super(url);
    }

    // Default constructor
    public JsonRPCClient() throws MalformedURLException {
        // You can set a default URL here or leave it to use the one from BitcoinJSONRPCClient
        super();
    }

    public Map<String, Object> createWallet(String name) throws GenericRpcException {
        return (Map<String, Object>) this.query("createwallet", name, false, false, "", false, false);
    }

    public Map<String, Object> loadWallet(String name) throws GenericRpcException {
        return (Map<String, Object>) this.query("loadwallet", name);
    }

    public Map<String, Object> unloadWallet(String name) throws GenericRpcException {
        return (Map<String, Object>) this.query("unloadwallet", name);
    }

    public List<Map<String, Object>> secondNode() throws GenericRpcException {
        return (List<Map<String, Object>>) this.query("getaddednodeinfo");
    }


}

public class Application extends Thread {
    static final Logger LOGGER = Logger.getLogger(Application.class.getName());

	/*public static void main(String[] args) throws Exception
	{
		System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-7s] %5$s %n");

		BitcoinJSONRPCClient client = new BitcoinJSONRPCClient();
		Util.ensureRunningOnChain(Chain.REGTEST, client);
		//Thread.sleep(5000);
		client.query("addnode", "localhost:2223", "add");
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
			LOGGER.info("hello");
			URL url = new URL("http://user:WLMClI3cZ3ghE3diSTK-ENHSenP0bnthnbYmrAg7hcM@127.0.0.1:8333/");

			// Create a client instance
			BitcoinJSONRPCClient bitcoinClient = new BitcoinJSONRPCClient(url);
			System.out.println("aaaaa " + client.getNetworkInfo());
			System.out.println("eeeee " + bitcoinClient.getNetworkInfo());
		} else {
			System.out.println("Timeout: Node did not connect within the expected time.");
		}

		//jsonRpcClient.secondNode();

		// Before you run the examples:
		// 1. make sure you have an empty regtest chain (e.g. delete the regtest folder in the bitcoin data path)
		// 2. make sure the bitcoin client is running
		// 3. make sure it is running on regtest

		//signRawTransactionWithKeyTest_P2SH_MultiSig(client);
		//signRawTransactionWithKeyTest_P2SH_P2WPKH(client);
	}*/


    /**
     * Signing a transaction to a P2SH-P2WPKH address (Pay-to-Witness-Public-Key-Hash)
     *
     * @return
     */
    static JsonObject signRawTransactionWithKeyTest_P2SH_P2WPKH(BitcoinJSONRPCClient client, String addr1) throws MalformedURLException {
        Random rand = new Random(); // create instance of Random class
        BigDecimal minAmount = BigDecimal.valueOf(0.0001); // Minimum amount
        BigDecimal maxAmount = BigDecimal.valueOf(10.0);

        LOGGER.info("=== Testing scenario: signRawTransactionWithKey (addrTmp -> addr2)");
        // Call createWallet function from JsonRPCClient


        LOGGER.info("hello");
        URL url = new URL("http://user:WLMClI3cZ3ghE3diSTK-ENHSenP0bnthnbYmrAg7hcM@127.0.0.1:8333/");

        // Create a client instance
        BitcoinJSONRPCClient bitcoinClient = new BitcoinJSONRPCClient(url);

        String addrTmp = client.getNewAddress();
        //LOGGER.info("Created address addr1: " + addr1);

        JsonRPCClient secondNode = new JsonRPCClient(url);
        String randomWalletName = Application.generateRandomString(10);
        secondNode.createWallet(randomWalletName);
        String addr2 = bitcoinClient.getNewAddress();
        LOGGER.info("Created address addr2: " + addr2);

        List<String> generatedBlocksHashes = client.generateToAddress(100 + rand.nextInt(1, 23), addrTmp);
        List<BitcoindRpcClient.Unspent> utxos = client.listUnspent(0, Integer.MAX_VALUE, addrTmp);
        LOGGER.info("Found " + utxos.size() + " UTXOs (unspent transaction outputs) belonging to addrTmp");

        BigDecimal amountToTransfer = generateRandomAmount(minAmount, maxAmount);
        System.out.println(client.getBalance() + " sending " + amountToTransfer);
        String sentRawTransactionID = client.sendToAddress(addr2, amountToTransfer);
        LOGGER.info("Sent signedRawTx (txID): " + sentRawTransactionID);
        BitcoindRpcClient.Transaction transactionObj = client.getTransaction(sentRawTransactionID);
        String transaction = transactionObj.toString().replaceFirst(addr2.toString(), addrTmp.toString());

        transaction = transaction.replaceFirst("confirmations=0", "confirmations=" + client.getBlockCount());
        System.out.println("transac details" + prettyPrintJson(transaction));
        System.out.println("ballll" + client.getBalance());

        try {
            Map<String, Object> result = secondNode.unloadWallet(randomWalletName);
            String warning = (String) result.get("warning");
            Application.LOGGER.info("Wallet unloaded: " + randomWalletName);
            if (warning != null) {
                Application.LOGGER.warning("Warning: " + warning);
            }
        } catch (GenericRpcException e) {
            Application.LOGGER.severe("Error unloading wallet: " + e.getMessage());
            //return; // Exit the function if an error occurs
        }
        Gson gson = new Gson();

// Parse the transaction JSON string into a JsonObject
        JsonObject transactionObject = gson.fromJson(transaction, JsonObject.class);
        transactionObject.addProperty("receiving_address", addr2);

        return transactionObject;


    }

    public static BigDecimal generateRandomAmount(BigDecimal min, BigDecimal max) {
        Random random = new Random();

        // Define the distribution of transaction amounts
        // For simplicity, you can define a distribution based on ranges and their corresponding probabilities
        // Adjust the probabilities based on your analysis of real-world Bitcoin transaction data
        double[] rangeProbabilities = {0.1, 0.2, 0.3, 0.2, 0.1, 0.05, 0.03, 0.02};

        // Define the ranges corresponding to the probabilities
        BigDecimal[] rangeMinimums = {
                BigDecimal.valueOf(0.0001),
                BigDecimal.valueOf(0.001),
                BigDecimal.valueOf(0.01),
                BigDecimal.valueOf(0.1),
                BigDecimal.valueOf(1.0),
                BigDecimal.valueOf(2.0),
                BigDecimal.valueOf(5.0),
                BigDecimal.valueOf(10.0)
        };

        // Select a range based on probabilities
        double randomNumber = random.nextDouble();
        double cumulativeProbability = 0.0;
        int selectedRangeIndex = 0;
        for (int i = 0; i < rangeProbabilities.length; i++) {
            cumulativeProbability += rangeProbabilities[i];
            if (randomNumber <= cumulativeProbability) {
                selectedRangeIndex = i;
                break;
            }
        }

        // Generate a random value within the selected range
        BigDecimal selectedRangeMin = rangeMinimums[selectedRangeIndex];
        BigDecimal selectedRangeMax = (selectedRangeIndex == rangeMinimums.length - 1) ? max : rangeMinimums[selectedRangeIndex + 1];
        BigDecimal randomBigDecimal = selectedRangeMin.add(new BigDecimal(random.nextDouble()).multiply(selectedRangeMax.subtract(selectedRangeMin)));

        return randomBigDecimal.setScale(8, BigDecimal.ROUND_HALF_UP);
    }

    private static String prettyPrintJson(String jsonData) {
        StringBuilder prettyJson = new StringBuilder();
        int indentLevel = 0;
        boolean inQuotes = false;

        for (char charValue : jsonData.toCharArray()) {
            switch (charValue) {
                case '{':
                case '[':
                    prettyJson.append(charValue);
                    prettyJson.append('\n');
                    indentLevel++;
                    addIndentation(indentLevel, prettyJson);
                    break;
                case '}':
                case ']':
                    prettyJson.append('\n');
                    indentLevel--;
                    addIndentation(indentLevel, prettyJson);
                    prettyJson.append(charValue);
                    break;
                case ',':
                    prettyJson.append(charValue);
                    if (!inQuotes) {
                        prettyJson.append('\n');
                        addIndentation(indentLevel, prettyJson);
                    }
                    break;
                case '"':
                    prettyJson.append(charValue);
                    inQuotes = !inQuotes;
                    break;
                default:
                    prettyJson.append(charValue);
            }
        }
        return prettyJson.toString();
    }

    public static String generateRandomString(int length) {
        // Define the characters that can be used in the random string
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        // Initialize a Random object
        Random random = new Random();

        // Initialize a StringBuilder to build the random string
        StringBuilder sb = new StringBuilder(length);

        // Loop to generate random characters and append them to the StringBuilder
        for (int i = 0; i < length; i++) {
            // Generate a random index within the range of characters
            int randomIndex = random.nextInt(characters.length());

            // Append the character at the random index to the StringBuilder
            sb.append(characters.charAt(randomIndex));
        }

        // Convert the StringBuilder to a String and return it
        return sb.toString();
    }

    private static void addIndentation(int indentLevel, StringBuilder stringBuilder) {
        for (int i = 0; i < indentLevel; i++) {
            stringBuilder.append("    "); // 4 spaces for each level of indentation
        }
    }

}