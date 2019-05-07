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

        String managementEndpoint = buildAkkaManagementEndpoint();
        String getClusterMembersUrl = managementEndpoint.concat("/cluster/members");
        return awsConfigHelper.sendGetRequest(getClusterMembersUrl);
    }


}
