Vagrant.configure('2') do |config|
    config.vm.box = "ubuntu/trusty64"
    config.ssh.insert_key = true
    config.vm.network "forwarded_port", guest: 9000, host: 9000
    config.vm.network "private_network", ip: "192.168.40.10"

    config.vm.provider "virtualbox" do |v|
      v.memory = 8192
      v.cpus = 2
    end


    if Dir.glob("#{File.dirname(__FILE__)}/.vagrant/machines/default/*/id").empty?

      config.vm.provision :shell, path: "init-script.sh"
    
    end
end
