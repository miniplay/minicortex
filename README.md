# MINICORTEX

### Description
[WIP] Minicortex is your own Docker Elastic Load Balancer!

### Installation
1. Clone repository: ```git clone https://github.com/miniplay/minicortex ```
2. Create config file: ``` cd minicortex && cp config_example.yml minicortex_config.yml ```
3. Complie and run: ``` sh compile_and_run.sh minicortex_config.yml ```

### Observers

### Config
| Option | Description  | DEFAULT |
|----------|---------------|-----------------|
DEBUG                               | description | true |
DOCKER_DEFAULT_DRIVER               | Docker driver used to provision machines | amazonec2 |
DOCKER_MIN_CONTAINERS               | Min number of containers provisioned | 1 |
DOCKER_MAX_CONTAINERS               | Max number of containers provisioned | 3 |
DOCKER_MAX_BOOTS_IN_LOOP            | Max container bootups on each balancer loop | 1 |
DOCKER_MAX_SHUTDOWNS_IN_LOOP        | Max container shutdowns on each balancer loop | 1 |
DOCKER_KILL_MODE                    | Container kill mode: SOFT or HARD | SOFT |
DOCKER_SOFT_KILL_PATH               | Directory where Minicortex will write the SOFT kill file | /tmp/ |
DOCKER_SOFT_KILL_FILENAME           | Filename of the SOFT kill file | container.die |
DOCKER_CONTAINER_HOSTNAME_BASENAME  | Basename for Minicortex containers | worker- |
AMAZONEC2_REGION                    | EC2 region where Minicortex will provision the containers | eu-west-1 |
AMAZONEC2_ACCESS_KEY                | EC2 Access Key | null |
AMAZONEC2_SECRET_KEY                | EC2 Secret Key | null |
AMAZONEC2_VPC_ID                    |  | null |
AMAZONEC2_ZONE                      |  | null |
AMAZONEC2_SSH_USER                  |  | null |
AMAZONEC2_INSTANCE_TYPE             |  | t2.micro |
AMAZONEC2_AMI                       | EC2 image used by Minicortex to provision containers | null |
AMAZONEC2_SUBNET_ID                 |  | null |
AMAZONEC2_SECURITY_GROUP            |  | null |
AMAZONEC2_PRIVATE_ADDRESS_ONLY      |  | true |
EB_ALLOW_PROVISION_CONTAINERS       | Is Minicortex allowed to provision containers? | false |
EB_TOLERANCE_THRESHOLD              | Tolerance threshold for the balancer | 3 |
STATSD_HOST                         | STATSD HOST | null |
STATSD_PORT                         | STATSD PORT | null |

#### Config appendix
