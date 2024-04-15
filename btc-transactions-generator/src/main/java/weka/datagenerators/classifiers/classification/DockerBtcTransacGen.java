/*
Supervisors:

Nida Meddouri nida.meddouri@epita.fr
Elloh Adja elloh.adja@epita.fr

Sami Mazirt mazirtsamicm@gmail.com
Jonathan Sa william.jonathan.sa@gmail.com
Edmond Nguefeu edmond.nguefeu@gmail.com
Alexis Lefrancois alexis.lefrancois@epita.fr
 */


package weka.datagenerators.classifiers.classification;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.async.ResultCallback.Adapter;
import com.github.dockerjava.api.command.ExecStartCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

public class DockerBtcTransacGen {

    public static void main(String[] args) throws IOException, InterruptedException {
        dockerMain("smazdat/btctransacgen:latest");
    }
    public static void dockerMain(String dockerImage) throws InterruptedException, IOException {
        System.out.println("Run Docker");

        // Docker parameters
        String containerName1 = "docker-bitcoin-node1-1";
        String containerName2 = "docker-bitcoin-node2-1";
        String containerFile = "/data/node1";

        // Get the Docker client
        System.out.println("Get Docker client");
        DockerClient dockerClient = getDockerClient();

        if(dockerClient == null)
            throw new IllegalStateException("Could not connect to docker !");

        // Check if the container node 1 is already running
        if (dockerContainerExists(containerName1, dockerClient)) {
            System.out.println("Container already exists, stopping");
            dockerStop(containerName1, dockerClient);
            dockerRm(containerName1, dockerClient);
        }

        // Check if the container node 2 is already running
        if (dockerContainerExists(containerName2, dockerClient)) {
            System.out.println("Container already exists, stopping");
            dockerStop(containerName2, dockerClient);
            dockerRm(containerName2, dockerClient);
        }

        // Check if the image exists
        if (!dockerImageExists(dockerImage, dockerClient)) {
            // Pull the image
            dockerPull(dockerImage, dockerClient);
        }

        // Run the container node 1
        // Node 1 port mappings
        List<int[]> node1PortMappings = Arrays.asList(
                new int[]{18444, 18444},
                new int[]{18443, 18443},
                new int[]{9997, 9997}
        );
        dockerRun(dockerImage, containerName1, dockerClient, node1PortMappings);
        // Run the container node 2
        // Node 2 port mappings
        List<int[]> node2PortMappings = Arrays.asList(
                new int[]{18454, 18444},
                new int[]{2223, 2223},
                new int[]{8333, 8333}
        );
        dockerRun(dockerImage, containerName2, dockerClient, node2PortMappings);

        dockerExec("bitcoind --fallbackfee=0.0002 -datadir=/data/node1/ -conf=/data/node1/bitcoin.conf -printtoconsole\n", containerName1, dockerClient);
        Thread.sleep(4000);
        dockerExec("bitcoind -port=2223 -rpcport=8333 -datadir=/data/node2/ -conf=/data/node2/bitcoin.conf -printtoconsole\n", containerName2, dockerClient);
        dockerExec("bitcoind -port=2223 -rpcport=8333 -datadir=/data/node2/ -conf=/data/node2/bitcoin.conf -printtoconsole\n", containerName2, dockerClient); // we need this line even if its duplicate, without it it doesn't work (maybe thread.sleep)


    }

    /**
     * Pull a docker image.
     *
     * @param dockerImage the docker image to pull
     * @param dockerClient the Docker client
     * @throws InterruptedException
     */
    public static void dockerPull(String dockerImage, DockerClient dockerClient) throws InterruptedException {
        System.out.println("Pull image " + dockerImage);
        try {
            dockerClient.pullImageCmd(dockerImage).exec(new PullImageResultCallback()).awaitCompletion();
        } catch (NotFoundException e) {
            throw new RuntimeException("Error while pulling image: " + dockerImage);
        }
    }

    /**
     * Check if a given Docker image exists locally.
     *
     * @param dockerImage the docker image to run
     * @param dockerClient the Docker client
     */
    public static boolean dockerImageExists(String dockerImage, DockerClient dockerClient) {
        System.out.println("Check if image " + dockerImage + " exists localy");
        List<Image> images = dockerClient.listImagesCmd().exec();
        for (Image image : images) {
            for (String repoTag : image.getRepoTags()) {
                if (repoTag.equals(dockerImage)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static DockerClient getDockerClient() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerTlsVerify(false)
                .withRegistryUsername("dockeruser")
                .build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(10))
                .responseTimeout(Duration.ofSeconds(30))
                .build();
        return DockerClientImpl.getInstance(config, httpClient);
    }

    /**
     * Stop the given container.
     *
     * @param containerName the name of the container
     * @param dockerClient the Docker client
     */
    public static void dockerStop(String containerName, DockerClient dockerClient) {
        // Stop container
        System.out.println("Stop the container " + containerName);
        dockerClient.killContainerCmd(containerName).exec();
    }

    /**
     * Remove the given docker image.
     *
     * @param containerName the name of the container
     * @param dockerClient the Docker client
     */
    public static void dockerRm(String containerName, DockerClient dockerClient) {
        // Remove container
        System.out.println("Remove the container " + containerName);
        dockerClient.removeContainerCmd(containerName).exec();
    }

    /**
     * Check if a container exists.
     *
     * @param containerName the name of the container
     * @param dockerClient the Docker client
     * @return true if the container exists, false otherwise
     */
    public static boolean dockerContainerExists(String containerName, DockerClient dockerClient) {
        System.out.println("Check if container " + containerName + " is already running");
        List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
        for (Container container : containers) {
            if (container.getNames()[0].equals("/" + containerName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Run a docker container.
     *
     * @param dockerImage the docker image to run
     * @param containerName the name of the container
     * @param dockerClient the Docker client
     */
    public static void dockerRun(String dockerImage, String containerName, DockerClient dockerClient, List<int[]> portMappings) {
        Ports portBindings = new Ports();

        for (int[] mapping : portMappings) {
            ExposedPort exposedPort = ExposedPort.tcp(mapping[1]);
            PortBinding portBinding = new PortBinding(Ports.Binding.bindPort(mapping[0]), exposedPort);
            portBindings.bind(exposedPort, portBinding.getBinding());
        }

        System.out.println("Creating Docker container with port bindings for " + containerName);
        dockerClient.createContainerCmd(dockerImage)
                .withName(containerName)
                // Explicitly specifying each exposed port
                .withExposedPorts(portMappings.stream().map(mapping -> ExposedPort.tcp(mapping[1])).toArray(ExposedPort[]::new))
                .withHostConfig(new HostConfig().withPortBindings(portBindings))
                .withTty(true)
                .exec();

        System.out.println("Starting Docker container " + containerName);
        dockerClient.startContainerCmd(containerName).exec();
    }

    /**
     * Exec a command in the container.
     *
     * @param command the command to execute
     * @param containerName the name of the container
     * @param dockerClient the Docker client
     * @return
     */
    public static Adapter<Frame> dockerExec(String command, String containerName, DockerClient dockerClient) throws InterruptedException {
        System.out.println("Execute " + command + " in " + containerName);
        String id = dockerClient.execCreateCmd(containerName)
                //.withAttachStdout(true)
                //.withAttachStderr(true)
                .withCmd("bash", "-c", command)
                .exec()
                .getId();
        ExecStartCmd start = dockerClient.execStartCmd(id);
        Adapter<Frame> handler = new Adapter<Frame>() {
            @Override
            public void onNext(Frame object) {
                super.onNext(object);
                System.out.println("Message from docker command: " + object);
            }

            @Override
            public void onError(Throwable throwable) {
                super.onError(throwable);
                throwable.printStackTrace();
            }
        };

        return start.exec(handler);
    }

    public static String dockerLogs(String containerName, DockerClient dockerClient) {
        StringBuilder logs = new StringBuilder();
        dockerClient.logContainerCmd(containerName)
                .withStdOut(true)
                .withStdErr(true)
                .exec(new ResultCallback.Adapter<>() {
                    @Override
                    public void onNext(Frame item) {
                        logs.append(new String(item.getPayload())).append("\n");
                    }
                });
        return logs.toString();
    }


    /**
     * docker Inspect IP address.
     *
     * @param containerName
     * @param dockerClient
     * @return the ip address of the container
     */
    public static String dockerInspectIP(String containerName, DockerClient dockerClient) {
        // Get Ip address
        System.out.println("Get IP address");
        ContainerNetwork network = dockerClient.inspectContainerCmd(containerName).exec().getNetworkSettings()
                .getNetworks().values().iterator().next();
        String ipAddress = network.getIpAddress();
        System.out.println("IP Address: " + ipAddress);
        return ipAddress;
    }
}
