@set consul_data_dir=D:\temp\consul-data
@rmdir %consul_data_dir% /s /q
@consul agent -server -bootstrap-expect=1 -data-dir=%consul_data_dir% -ui -client=0.0.0.0 -bind=%1