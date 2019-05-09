package helpers;


public class AkkaClusterManagement {

    AwsConfigHelper awsConfigHelper;
    String hostName;

    public  AkkaClusterManagement(AwsConfigHelper helper, String _hostName){
        awsConfigHelper = helper;
        hostName = _hostName;
    }

    private String buildAkkaManagementEndpoint(){
        StringBuilder builder = new StringBuilder("http://");
        builder.append(hostName).append(":").append(awsConfigHelper.getAkkaManagementPort());
        return  builder.toString();
    }

    public  String getClusterMembers(){

        StringBuilder managementEndpoint = new StringBuilder(buildAkkaManagementEndpoint());
        String getClusterMembersUrl = managementEndpoint.append("/cluster/members").toString();
        return awsConfigHelper.sendGetRequest(getClusterMembersUrl);
    }

}
