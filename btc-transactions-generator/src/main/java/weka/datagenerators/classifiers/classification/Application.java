package weka.datagenerators.classifiers.classification;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import com.google.gson.*;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback.Adapter;
import com.github.dockerjava.api.model.Frame;

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

    public Map<Boolean, Object> setfee(String fee) throws GenericRpcException {
        return (Map<Boolean, Object>) this.query("settxfee", fee);
    }



}

public class Application extends Thread {
    static final Logger LOGGER = Logger.getLogger(Application.class.getName());

    /**
     * Signing a transaction to a P2SH-P2WPKH address (Pay-to-Witness-Public-Key-Hash)
     *
     * @return
     */
    static JsonObject signRawTransactionWithKeyTest_P2SH_P2WPKH(BitcoinJSONRPCClient client, String addr1) throws MalformedURLException {
        Random rand = new Random(); // create instance of Random class
        BigDecimal minAmount = BigDecimal.valueOf(0.0001); // Minimum amount
        BigDecimal maxAmount = BigDecimal.valueOf(10.0);
        BigDecimal minFeeAmount = BigDecimal.valueOf(0.00001); // Minimum amount
        BigDecimal maxFeeAmount = BigDecimal.valueOf(0.0004);

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
        BigDecimal amountToTransfer = generateRandomAmount(minAmount, maxAmount);

        List<String> generatedBlocksHashes = client.generateToAddress(100 + rand.nextInt(1, 23), addrTmp);
        List<BitcoindRpcClient.Unspent> utxos = client.listUnspent(0, Integer.MAX_VALUE, addrTmp);
        LOGGER.info("Found " + utxos.size() + " UTXOs (unspent transaction outputs) belonging to addrTmp");

        BitcoindRpcClient.Unspent selectedUtxo = utxos.get(0);

        LOGGER.info("Selected UTXO which will be sent from addrTmp to addr2: " + selectedUtxo);
        //set fee ?
        BitcoindRpcClient.ExtendedTxInput inputP2SH_P2WPKH = new BitcoindRpcClient.ExtendedTxInput(
                selectedUtxo.txid(),
                selectedUtxo.vout(),
                selectedUtxo.scriptPubKey(),
                amountToTransfer,
                selectedUtxo.redeemScript(),
                selectedUtxo.witnessScript());
        LOGGER.info("inputP2SH_P2WPKH txid: " + 			inputP2SH_P2WPKH.txid());
        LOGGER.info("inputP2SH_P2WPKH vout: " + 			inputP2SH_P2WPKH.vout());
        LOGGER.info("inputP2SH_P2WPKH scriptPubKey: " + 	inputP2SH_P2WPKH.scriptPubKey());
        LOGGER.info("inputP2SH_P2WPKH redeemScript: " + 	inputP2SH_P2WPKH.redeemScript());
        LOGGER.info("inputP2SH_P2WPKH witnessScript: " + 	inputP2SH_P2WPKH.witnessScript());
        LOGGER.info("inputP2SH_P2WPKH amount: " + 			inputP2SH_P2WPKH.amount());

        BitcoinRawTxBuilder rawTxBuilder = new BitcoinRawTxBuilder(client);
        rawTxBuilder.in(inputP2SH_P2WPKH);

        String tx1ID = client.sendToAddress(addr2, amountToTransfer);
        LOGGER.info("UTXO sent to P2SH-multiSigAddr, tx1 ID: " + tx1ID);


        // Found no other reliable way to estimate the fee in a test
        // Therefore, setting the fee for this tx 200 satoshis (what appears to be the min relay fee)
        //BigDecimal estimatedFee = BigDecimal.valueOf(0.00000200);
        BigDecimal txToAddr2Amount = selectedUtxo.amount();//.subtract(estimatedFee);
        rawTxBuilder.out(addr2, txToAddr2Amount);

        LOGGER.info("unsignedRawTx in amount: " + selectedUtxo.amount());
        LOGGER.info("unsignedRawTx out amount: " + txToAddr2Amount);

        String unsignedRawTxHex = rawTxBuilder.create();

        LOGGER.info("Created unsignedRawTx from addrTmp to addr2: " + unsignedRawTxHex);
        // Sign tx
        BitcoindRpcClient.SignedRawTransaction srTx = client.signRawTransactionWithKey(
                unsignedRawTxHex,
                Arrays.asList(client.dumpPrivKey(addrTmp)), // addrTmp is sending, so we need to sign with the private key of addrTmp
                Arrays.asList(inputP2SH_P2WPKH),
                null);
        LOGGER.info("signedRawTx hex: " + srTx.hex());
        LOGGER.info("signedRawTx complete: " + srTx.complete());

        List<BitcoindRpcClient.RawTransactionSigningOrVerificationError> errors = srTx.errors();
        if (errors != null)
        {
            LOGGER.severe("Found errors when signing");

            for (BitcoindRpcClient.RawTransactionSigningOrVerificationError error : errors)
            {
                LOGGER.severe("Error: " + error);
            }
        }







        //System.out.println(client.getBalance() + " sending " + amountToTransfer);
        String sentRawTransactionID = client.sendToAddress(addr2, amountToTransfer);
        //LOGGER.info("Sent signedRawTx (txID): " + sentRawTransactionID);
        //String sentRawTransactionID = client.sendRawTransaction(srTx.hex());
        BitcoindRpcClient.Transaction transactionObj = client.getTransaction(sentRawTransactionID);

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
        String transaction = transactionObj.toString().replaceFirst(addr2.toString(), addrTmp.toString());
        transaction = transaction.replaceFirst("confirmations=0", "confirmations=" + client.getBlockCount());
        //System.out.println("transac details" + prettyPrintJson(transaction));
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
                BigDecimal.valueOf(0.0005),
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

    // Generates a random fee in BTC per kilobyte within a specified range, using BigDecimal for precision
    public static BigDecimal generateRandomFeeBTC(BigDecimal minFeeBTC, BigDecimal maxFeeBTC) {
        if (minFeeBTC.compareTo(maxFeeBTC) >= 0) {
            throw new IllegalArgumentException("maxFeeBTC must be greater than minFeeBTC");
        }
        Random random = new Random();
        BigDecimal randomBigDecimal = minFeeBTC.add(new BigDecimal(random.nextDouble()).multiply(maxFeeBTC.subtract(minFeeBTC)));
        // Ensure the scale is set to 8 decimal places, rounding half up
        return randomBigDecimal.setScale(8, RoundingMode.HALF_UP);
    }

    public static String prettyPrintJson(String jsonData) {
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