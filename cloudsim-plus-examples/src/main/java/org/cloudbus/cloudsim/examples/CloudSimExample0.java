package org.cloudbus.cloudsim.examples;

import org.cloudsimplus.util.tablebuilder.CloudletsTableBuilderHelper;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSimple;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterSimple;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.DatacenterCharacteristicsSimple;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.HostSimple;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.schedulers.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.VmSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.resources.Bandwidth;
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple;
import org.cloudbus.cloudsim.resources.Ram;
import org.cloudbus.cloudsim.schedulers.CloudletSchedulerSpaceShared;

/**
 * A minimal example showing how to create a data center with 1 host and run 2
 * cloudlets on it.
 *
 * @author Manoel Campos da Silva Filho
 */
public class CloudSimExample0 {
    private List<Cloudlet> cloudletList;
    private List<Vm> vmList;
    private int numberOfCreatedCloudlets = 0;
    private int numberOfCreatedVms = 0;
    private int numberOfCreatedHosts = 0;

    /**
     * Starts the simulation.
     * @param args
     */
    public static void main(String[] args) {
        new CloudSimExample0();
    }

    /**
     * Default constructor where the simulation is built.
     */
    public CloudSimExample0() {
        Log.printLine("Starting Minimal Example ...");
        try {
            this.vmList = new ArrayList<>();
            this.cloudletList = new ArrayList<>();
            //Number of cloud customers
            int numberOfCloudUsers = 1;
            boolean traceEvents = false;

            CloudSim.init(numberOfCloudUsers, Calendar.getInstance(), traceEvents);

            Datacenter datacenter0 = createDatacenter("Datacenter0");

            /*Creates a Broker accountable for submission of VMs and Cloudlets
            on behalf of a given cloud user (customer).*/
            DatacenterBroker broker0 = new DatacenterBrokerSimple("Broker0");

            Vm vm0 = createVm(broker0);
            this.vmList.add(vm0);
            broker0.submitVmList(vmList);

            /*Creates Cloudlets that represent applications to be run inside a VM.*/
            Cloudlet cloudlet0 = createCloudlet(broker0, vm0);
            this.cloudletList.add(cloudlet0);
            Cloudlet cloudlet1 = createCloudlet(broker0, vm0);
            this.cloudletList.add(cloudlet1);
            broker0.submitCloudletList(cloudletList);

            /*Starts the simulation and waits all cloudlets to be executed*/
            CloudSim.startSimulation();

            //Finishes the simulation
            CloudSim.stopSimulation();

            /*Prints results when the simulation is over
            (you can use your own code here to print what you want from this cloudlet list)*/
            List<Cloudlet> finishedCloudlets = broker0.getCloudletsFinishedList();
            new CloudletsTableBuilderHelper(finishedCloudlets).build();
            Log.printLine("Minimal Example finished!");
        } catch (RuntimeException e) {
            Log.printFormattedLine("Simulation finished due to unexpected error: %s", e);
        }
    }

    private DatacenterSimple createDatacenter(String name) {
        List<Host> hostList = new ArrayList<>();
        Host host0 = createHost();
        hostList.add(host0);

        //Defines the characteristics of the data center
        double cost = 3.0; // the cost of using processing in this datacenter
        double costPerMem = 0.05; // the cost of using memory in this datacenter
        double costPerStorage = 0.001; // the cost of using storage in this datacenter
        double costPerBw = 0.0; // the cost of using bw in this datacenter

        DatacenterCharacteristics characteristics =
            new DatacenterCharacteristicsSimple(hostList)
                .setCostPerSecond(cost)
                .setCostPerMem(costPerMem)
                .setCostPerStorage(costPerStorage)
                .setCostPerBw(costPerBw);

        return new DatacenterSimple(name, characteristics, new VmAllocationPolicySimple(hostList));
    }

    private Host createHost() {
        int  mips = 1000; // capacity of each CPU core (in Million Instructions per Second)
        long  ram = 2048; // host memory (MB)
        long storage = 1000000; // host storage (MB)
        long bw = 10000; //in Megabits/s

        List<Pe> pesList = new ArrayList<>(); //List of CPU cores

        /*Creates the Host's CPU cores and defines the provisioner
        used to allocate each core for requesting VMs.*/
        pesList.add(new PeSimple(0, new PeProvisionerSimple(mips)));

        return new HostSimple(numberOfCreatedHosts++, storage, pesList)
                .setRamProvisioner(new ResourceProvisionerSimple(new Ram(ram)))
                .setBwProvisioner(new ResourceProvisionerSimple(new Bandwidth(bw)))
                .setVmScheduler(new VmSchedulerTimeShared(pesList));
    }

    private Vm createVm(DatacenterBroker broker) {
        double mips = 1000;
        long   storage = 10000; // vm image size (MB)
        int    ram = 512; // vm memory (MB)
        long   bw = 1000; // vm bandwidth (Megabits/s)
        int    pesNumber = 1; // number of CPU cores

        return new VmSimple(numberOfCreatedVms++, mips, pesNumber)
                .setBroker(broker)
                .setRam(ram)
                .setBw(bw)
                .setSize(storage)
                .setCloudletScheduler(new CloudletSchedulerSpaceShared());
    }

    private Cloudlet createCloudlet(DatacenterBroker broker, Vm vm) {
        long length = 10000; //in Million Structions (MI)
        long fileSize = 300; //Size (in bytes) before execution
        long outputSize = 300; //Size (in bytes) after execution
        int  numberOfCpuCores = vm.getNumberOfPes(); //cloudlet will use all the VM's CPU cores

        //Defines how CPU, RAM and Bandwidth resources are used
        //Sets the same utilization model for all these resources.
        UtilizationModel utilization = new UtilizationModelFull();

        Cloudlet cloudlet
                = new CloudletSimple(
                        numberOfCreatedCloudlets++, length, numberOfCpuCores)
                        .setCloudletFileSize(fileSize)
                        .setCloudletOutputSize(outputSize)
                        .setUtilizationModel(utilization)
                        .setBroker(broker)
                        .setVmId(vm.getId());

        return cloudlet;
    }

}
