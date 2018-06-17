package org.swarmer.json;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeploymentContainerCfg implements Cloneable {
    private String consulServiceName;
    private String consulUrl;
    private File destFolder;
    private String filePattern;
    private String jvmParams;
    private String name;
    private File srcFolder;
    private String watchKeyHash;
    private List<SwarmDeploymentCfg> swarmDeploymentCfgs;

    public DeploymentContainerCfg(@JsonProperty("name") String name,
                                  @JsonProperty("src_folder") String srcFolder,
                                  @JsonProperty("dest_folder") String destFolder,
                                  @JsonProperty("file_pattern") String filePattern,
                                  @JsonProperty("jvm_params") String jvmParams,
                                  @JsonProperty("consul_url") String consulUrl,
                                  @JsonProperty("consul_service_name") String consulServiceName,
                                  @JsonProperty("swarm_deployment_list") List<SwarmDeploymentCfg> swarmDeploymentCfgs) {
        this.consulServiceName = consulServiceName;
        this.consulUrl = consulUrl;
        this.destFolder = new File(destFolder);
        this.filePattern = filePattern;
        this.jvmParams = jvmParams;
        this.name = name;
        this.srcFolder = new File(srcFolder);
        this.swarmDeploymentCfgs = (swarmDeploymentCfgs != null) ? swarmDeploymentCfgs : new ArrayList<>();
    }

    public void addSwarmDeploymentCfg(SwarmDeploymentCfg swarmDeploymentCfg) {
        swarmDeploymentCfgs.add(swarmDeploymentCfg);
    }

    public void clearSwarmDeploymentList() {
        swarmDeploymentCfgs.clear();
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String toString() {
        return "DeploymentContainerCfg{" +
                "consulServiceName='" + consulServiceName + '\'' +
                ", consulUrl='" + consulUrl + '\'' +
                ", destFolder='" + destFolder + '\'' +
                ", filePattern='" + filePattern + '\'' +
                ", jvmParams='" + jvmParams + '\'' +
                ", name='" + name + '\'' +
                ", srcFolder='" + srcFolder + '\'' +
                ", swarmDeploymentCfgs=" + swarmDeploymentCfgs +
                '}';
    }

    @JsonGetter("consul_service_name")
    public String getConsulServiceName() {
        return consulServiceName;
    }

    @JsonGetter("consul_url")
    public String getConsulUrl() {
        return consulUrl;
    }

    @JsonIgnore
    public File getDestFolder() {
        return destFolder;
    }

    @JsonGetter("dest_folder")
    public String getDestFolderPath() {
        return destFolder.getAbsolutePath();
    }

    @JsonGetter("file_pattern")
    public String getFilePattern() {
        return filePattern;
    }

    @JsonGetter("jvm_params")
    public String getJvmParams() {
        return jvmParams;
    }

    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JsonIgnore
    public File getSrcFolder() {
        return srcFolder;
    }

    @JsonGetter("src_folder")
    public String getSrcFolderPath() {
        return srcFolder.getAbsolutePath();
    }

    public SwarmDeploymentCfg getSwarmDeploymentCfg(int index) {
        return index < swarmDeploymentCfgs.size() ? swarmDeploymentCfgs.get(index) : null;
    }

    @JsonIgnore
    public String getWatchKeyHash() {
        return watchKeyHash;
    }

    @JsonIgnore
    public void setWatchKeyHash(String watchKeyHash) {
        this.watchKeyHash = watchKeyHash;
    }

    public int swarmDeploymentCfgsSize() {
        return swarmDeploymentCfgs.size();
    }

    @JsonGetter("swarm_deployment_list")
    protected List<SwarmDeploymentCfg> swarmDeploymentCfgList() {
        return swarmDeploymentCfgs;
    }
}